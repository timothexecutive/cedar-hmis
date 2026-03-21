package ke.cedar.hmis.billing;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Path("/api/billing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BillingResource {

    @Inject BillingService billingService;
    @Inject MpesaService   mpesaService;

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Cedar HMIS — Billing module is alive!";
    }

    // ── INSURANCE PROVIDERS ───────────────────────────
    @GET
    @Path("/providers")
    @RolesAllowed({"CASHIER", "ADMIN", "RECEPTIONIST"})
    public List<InsuranceProvider> getProviders() {
        return billingService.getProviders();
    }

    // ── INVOICES ──────────────────────────────────────
    @POST
    @Path("/invoices")
    @Transactional
    @RolesAllowed({"CASHIER", "ADMIN", "RECEPTIONIST"})
    public Response createInvoice(Map<String, Object> body) {
        Long patientId   = Long.parseLong(
            body.get("patientId").toString());
        Long visitId     = body.get("visitId") != null ?
            Long.parseLong(body.get("visitId").toString()) : null;
        Long admissionId = body.get("admissionId") != null ?
            Long.parseLong(body.get("admissionId").toString()) : null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
            (List<Map<String, Object>>) body.get("items");

        Invoice invoice = billingService.createInvoice(
            patientId, visitId, admissionId,
            (String) body.get("invoiceType"),
            (String) body.get("createdBy"),
            items);
        return Response.status(201).entity(invoice).build();
    }

    @GET
    @Path("/invoices/pending")
    @RolesAllowed({"CASHIER", "ADMIN"})
    public List<Invoice> getPending() {
        return billingService.getPendingInvoices();
    }

    @GET
    @Path("/invoices/patient/{patientId}")
    @RolesAllowed({"CASHIER", "ADMIN", "DOCTOR", "NURSE"})
    public List<Invoice> getPatientInvoices(
            @PathParam("patientId") Long patientId) {
        return billingService.getPatientInvoices(patientId);
    }

    @GET
    @Path("/invoices/{invoiceId}/items")
    @RolesAllowed({"CASHIER", "ADMIN", "DOCTOR"})
    public List<InvoiceItem> getItems(
            @PathParam("invoiceId") Long invoiceId) {
        return billingService.getInvoiceItems(invoiceId);
    }

    // ── PAYMENTS ──────────────────────────────────────
    @POST
    @Path("/invoices/{invoiceId}/pay")
    @Transactional
    @RolesAllowed({"CASHIER", "ADMIN"})
    public Response recordPayment(
            @PathParam("invoiceId") Long invoiceId,
            Map<String, String> body) {
        Payment payment = billingService.recordPayment(
            invoiceId,
            body.get("method"),
            new BigDecimal(body.get("amount")),
            body.get("referenceNo"),
            body.get("cashier"));
        return Response.status(201).entity(payment).build();
    }

    @GET
    @Path("/invoices/{invoiceId}/payments")
    @RolesAllowed({"CASHIER", "ADMIN"})
    public List<Payment> getPayments(
            @PathParam("invoiceId") Long invoiceId) {
        return billingService.getPayments(invoiceId);
    }

    // ── M-PESA STK PUSH ───────────────────────────────
    @POST
    @Path("/mpesa/stk-push")
    @Transactional
    @RolesAllowed({"CASHIER", "ADMIN"})
    public Response stkPush(Map<String, String> body) {
        Long invoiceId = Long.parseLong(body.get("invoiceId"));
        String phone   = body.get("phone");
        String cashier = body.get("cashier");

        // Create pending payment record first
        Payment payment = billingService.initiateMpesaPayment(
            invoiceId, phone, cashier);

        // Get invoice for amount
        Invoice invoice = Invoice.findById(invoiceId);

        // Initiate STK push
        String result = mpesaService.initiateSTKPush(
            phone,
            invoice.balance.intValue(),
            invoice.invoiceNo,
            "Cedar Hospital Payment");

        // Save checkoutRequestId so callback can find this payment
        try {
            if (result.contains("CheckoutRequestID")) {
                String checkoutId = result
                    .split("\"CheckoutRequestID\":\"")[1]
                    .split("\"")[0];
                payment.referenceNo = checkoutId;
                payment.persist();
            }
        } catch (Exception e) {
            System.err.println("Could not extract CheckoutRequestID: "
                + e.getMessage());
        }

        return Response.ok(result).build();
    }

    // ── M-PESA CALLBACK — public, Safaricom calls this ─
    @POST
    @Path("/mpesa/callback")
    @Transactional
    @PermitAll
    public Response mpesaCallback(Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>)
                payload.get("Body");

            @SuppressWarnings("unchecked")
            Map<String, Object> stkCallback = (Map<String, Object>)
                body.get("stkCallback");

            String merchantRequestId = (String) stkCallback.get("MerchantRequestID");
            String checkoutRequestId = (String) stkCallback.get("CheckoutRequestID");
            Integer resultCode       = (Integer) stkCallback.get("ResultCode");
            String resultDesc        = (String) stkCallback.get("ResultDesc");

            // Log the callback
            MpesaCallback log       = new MpesaCallback();
            log.merchantRequestId   = merchantRequestId;
            log.checkoutRequestId   = checkoutRequestId;
            log.resultCode          = resultCode;
            log.resultDesc          = resultDesc;
            log.rawPayload          = payload.toString();

            if (resultCode == 0) {
                @SuppressWarnings("unchecked")
                Map<String, Object> callbackMetadata =
                    (Map<String, Object>) stkCallback.get("CallbackMetadata");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items =
                    (List<Map<String, Object>>) callbackMetadata.get("Item");

                String mpesaReceiptNo = null;
                Double amount         = null;
                String phoneNumber    = null;

                for (Map<String, Object> item : items) {
                    String name  = (String) item.get("Name");
                    Object value = item.get("Value");
                    if (value == null) continue;

                    switch (name) {
                        case "MpesaReceiptNumber" -> mpesaReceiptNo = value.toString();
                        case "Amount"             -> amount = Double.parseDouble(value.toString());
                        case "PhoneNumber"        -> phoneNumber = value.toString();
                    }
                }

                log.mpesaReceipt = mpesaReceiptNo;
                log.amount       = amount != null ?
                    new java.math.BigDecimal(amount.toString()) : null;
                log.phoneNumber  = phoneNumber;
                log.processed    = true;

                // Delegate to service — handles balance cap + status
                billingService.verifyMpesaPayment(
                    checkoutRequestId,
                    mpesaReceiptNo,
                    log.amount);
            }

            log.persist();

            return Response.ok(
                "{\"ResultCode\":0,\"ResultDesc\":\"Accepted\"}"
            ).build();

        } catch (Exception e) {
            System.err.println("M-Pesa callback error: " + e.getMessage());
            return Response.ok(
                "{\"ResultCode\":0,\"ResultDesc\":\"Accepted\"}"
            ).build();
        }
    }

    // ── INSURANCE CLAIMS ──────────────────────────────
    @POST
    @Path("/claims")
    @Transactional
    @RolesAllowed({"CASHIER", "ADMIN"})
    public Response submitClaim(Map<String, String> body) {
        InsuranceClaim claim = billingService.submitClaim(
            Long.parseLong(body.get("invoiceId")),
            Long.parseLong(body.get("providerId")),
            body.get("memberNo"),
            new BigDecimal(body.get("amount")));
        return Response.status(201).entity(claim).build();
    }

    @GET
    @Path("/claims/status/{status}")
    @RolesAllowed({"CASHIER", "ADMIN"})
    public List<InsuranceClaim> getClaimsByStatus(
            @PathParam("status") String status) {
        return billingService.getClaimsByStatus(status);
    }

    @PUT
    @Path("/claims/{claimId}/status")
    @Transactional
    @RolesAllowed({"CASHIER", "ADMIN"})
    public InsuranceClaim updateClaimStatus(
            @PathParam("claimId") Long claimId,
            Map<String, String> body) {
        return billingService.updateClaimStatus(
            claimId,
            body.get("status"),
            body.get("approvedAmount") != null ?
                new BigDecimal(body.get("approvedAmount")) : null,
            body.get("refNo"),
            body.get("rejectionReason"));
    }
}
