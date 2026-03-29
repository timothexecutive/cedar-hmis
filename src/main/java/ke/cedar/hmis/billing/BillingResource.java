package ke.cedar.hmis.billing;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.appointments.AppointmentService;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Path("/api/billing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BillingResource {

    @Inject BillingService     billingService;
    @Inject MpesaService       mpesaService;
    @Inject AppointmentService appointmentService;
    @Inject JsonWebToken       jwt;

    // ── Helper: safe JWT userId extraction ───────────
    private Long getUserId() {
        Object claim = jwt.getClaim("userId");
        return claim != null ?
            Long.parseLong(claim.toString()) : null;
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Cedar HMIS — Billing module is alive!";
    }

    @GET
    @Path("/providers")
    @RolesAllowed({"CASHIER","ADMIN","RECEPTIONIST"})
    public List<InsuranceProvider> getProviders() {
        return billingService.getProviders();
    }

    @POST
    @Path("/invoices")
    @Transactional
    @RolesAllowed({"CASHIER","ADMIN","RECEPTIONIST"})
    public Response createInvoice(
            Map<String, Object> body) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        Long patientId  = Long.parseLong(
            body.get("patientId").toString());
        Long visitId    = body.get("visitId") != null ?
            Long.parseLong(
                body.get("visitId").toString()) : null;
        Long admissionId =
            body.get("admissionId") != null ?
            Long.parseLong(
                body.get("admissionId").toString()) : null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
            (List<Map<String, Object>>) body.get("items");

        Invoice invoice = billingService.createInvoice(
            patientId, visitId, admissionId,
            (String) body.get("invoiceType"),
            userName, items,
            userId, userName);
        return Response.status(201)
            .entity(invoice).build();
    }

    @GET
    @Path("/invoices/pending")
    @RolesAllowed({"CASHIER","ADMIN"})
    public List<Invoice> getPending() {
        return billingService.getPendingInvoices();
    }

    @GET
    @Path("/invoices/patient/{patientId}")
    @RolesAllowed({"CASHIER","ADMIN","DOCTOR","NURSE"})
    public List<Invoice> getPatientInvoices(
            @PathParam("patientId") Long patientId) {
        return billingService.getPatientInvoices(patientId);
    }

    @GET
    @Path("/invoices/{invoiceId}/items")
    @RolesAllowed({"CASHIER","ADMIN","DOCTOR"})
    public List<InvoiceItem> getItems(
            @PathParam("invoiceId") Long invoiceId) {
        return billingService.getInvoiceItems(invoiceId);
    }

    @POST
    @Path("/invoices/{invoiceId}/pay")
    @Transactional
    @RolesAllowed({"CASHIER","ADMIN"})
    public Response recordPayment(
            @PathParam("invoiceId") Long invoiceId,
            Map<String, String> body) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        Payment payment = billingService.recordPayment(
            invoiceId,
            body.get("method"),
            new BigDecimal(body.get("amount")),
            body.get("referenceNo"),
            userName,
            userId, userName);
        return Response.status(201)
            .entity(payment).build();
    }

    @GET
    @Path("/invoices/{invoiceId}/payments")
    @RolesAllowed({"CASHIER","ADMIN"})
    public List<Payment> getPayments(
            @PathParam("invoiceId") Long invoiceId) {
        return billingService.getPayments(invoiceId);
    }

    @POST
    @Path("/mpesa/stk-push")
    @Transactional
    @RolesAllowed({"CASHIER","ADMIN"})
    public Response stkPush(Map<String, String> body) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        Long invoiceId  = Long.parseLong(
            body.get("invoiceId"));
        String phone    = body.get("phone");

        Payment payment = billingService
            .initiateMpesaPayment(
                invoiceId, phone, userName,
                userId, userName);

        String checkoutRequestId =
            mpesaService.initiateSTKPush(
                phone,
                payment.amount.intValue(),
                "INV-" + invoiceId,
                "Cedar Hospital Payment");

        payment.referenceNo = checkoutRequestId;
        payment.persist();

        return Response.status(201).entity(Map.of(
            "message",           "STK Push sent to " + phone,
            "checkoutRequestId", checkoutRequestId,
            "invoiceId",         invoiceId,
            "amount",            payment.amount,
            "paymentMethod",     "MPESA",
            "verified",          false,
            "receiptNo",         payment.receiptNo != null
                                    ? payment.receiptNo : "",
            "status", "Waiting for customer to enter PIN"
        )).build();
    }

    // ── M-Pesa Callback ───────────────────────────────
    // Called by Safaricom after customer enters PIN
    // Handles BOTH billing invoice payments
    // AND appointment online payments
    @POST
    @Path("/mpesa/callback")
    @Transactional
    @PermitAll
    public Response mpesaCallback(
            Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body =
                (Map<String, Object>) payload.get("Body");
            @SuppressWarnings("unchecked")
            Map<String, Object> stkCallback =
                (Map<String, Object>) body.get("stkCallback");

            String merchantRequestId =
                (String) stkCallback.get("MerchantRequestID");
            String checkoutRequestId =
                (String) stkCallback.get("CheckoutRequestID");
            Integer resultCode =
                (Integer) stkCallback.get("ResultCode");
            String resultDesc =
                (String) stkCallback.get("ResultDesc");

            MpesaCallback log     = new MpesaCallback();
            log.merchantRequestId = merchantRequestId;
            log.checkoutRequestId = checkoutRequestId;
            log.resultCode        = resultCode;
            log.resultDesc        = resultDesc;
            log.rawPayload        = payload.toString();

            if (resultCode == 0) {
                @SuppressWarnings("unchecked")
                Map<String, Object> callbackMetadata =
                    (Map<String, Object>) stkCallback
                    .get("CallbackMetadata");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items =
                    (List<Map<String, Object>>)
                    callbackMetadata.get("Item");

                String mpesaReceiptNo = null;
                Double amount         = null;
                String phoneNumber    = null;

                for (Map<String, Object> item : items) {
                    String name  = (String) item.get("Name");
                    Object value = item.get("Value");
                    if (value == null) continue;
                    switch (name) {
                        case "MpesaReceiptNumber" ->
                            mpesaReceiptNo =
                                value.toString();
                        case "Amount" ->
                            amount = Double.parseDouble(
                                value.toString());
                        case "PhoneNumber" ->
                            phoneNumber = value.toString();
                    }
                }

                log.mpesaReceipt = mpesaReceiptNo;
                log.amount       = amount != null ?
                    new BigDecimal(amount.toString()) : null;
                log.phoneNumber  = phoneNumber;
                log.processed    = true;

                // ── 1. Try billing invoice payment ────
                billingService.verifyMpesaPayment(
                    checkoutRequestId,
                    mpesaReceiptNo,
                    log.amount);

                // ── 2. Try appointment payment ────────
                // confirmOnlinePayment() silently skips
                // if no appointment matches this ID
                appointmentService.confirmOnlinePayment(
                    checkoutRequestId,
                    mpesaReceiptNo);
            }

            log.persist();
            return Response.ok(
                "{\"ResultCode\":0," +
                "\"ResultDesc\":\"Accepted\"}")
                .build();

        } catch (Exception e) {
            System.err.println(
                "M-Pesa callback error: " +
                e.getMessage());
            // Always return 200 to Safaricom
            // even on errors — otherwise they retry
            return Response.ok(
                "{\"ResultCode\":0," +
                "\"ResultDesc\":\"Accepted\"}")
                .build();
        }
    }

    @POST
    @Path("/claims")
    @Transactional
    @RolesAllowed({"CASHIER","ADMIN"})
    public Response submitClaim(
            Map<String, String> body) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        InsuranceClaim claim = billingService.submitClaim(
            Long.parseLong(body.get("invoiceId")),
            Long.parseLong(body.get("providerId")),
            body.get("memberNo"),
            new BigDecimal(body.get("amount")),
            userId, userName);
        return Response.status(201)
            .entity(claim).build();
    }

    @GET
    @Path("/claims/status/{status}")
    @RolesAllowed({"CASHIER","ADMIN"})
    public List<InsuranceClaim> getClaimsByStatus(
            @PathParam("status") String status) {
        return billingService.getClaimsByStatus(status);
    }

    @PUT
    @Path("/claims/{claimId}/status")
    @Transactional
    @RolesAllowed({"CASHIER","ADMIN"})
    public InsuranceClaim updateClaimStatus(
            @PathParam("claimId") Long claimId,
            Map<String, String> body) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        return billingService.updateClaimStatus(
            claimId,
            body.get("status"),
            body.get("approvedAmount") != null ?
                new BigDecimal(
                    body.get("approvedAmount")) : null,
            body.get("refNo"),
            body.get("rejectionReason"),
            userId, userName);
    }

    @PUT
    @Path("/invoices/{invoiceId}/void")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response voidInvoice(
            @PathParam("invoiceId") Long invoiceId,
            Map<String, String> body) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        Invoice invoice = billingService.voidInvoice(
            invoiceId,
            body.get("reason"),
            userName,
            userId, userName);
        return Response.ok(invoice).build();
    }

    @POST
    @Path("/sessions/open")
    @Transactional
    @RolesAllowed({"CASHIER","ADMIN"})
    public Response openSession(
            Map<String, String> body) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        CashierSession session = billingService.openSession(
            userName,
            new BigDecimal(body.get("openingFloat")),
            userId, userName);
        return Response.status(201)
            .entity(session).build();
    }

    @PUT
    @Path("/sessions/{sessionId}/close")
    @Transactional
    @RolesAllowed({"CASHIER","ADMIN"})
    public Response closeSession(
            @PathParam("sessionId") Long sessionId,
            Map<String, String> body) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        CashierSession session = billingService.closeSession(
            sessionId,
            new BigDecimal(body.get("actualCash")),
            userId, userName);
        return Response.ok(session).build();
    }

    @GET
    @Path("/sessions/open")
    @RolesAllowed({"CASHIER","ADMIN"})
    public List<CashierSession> getOpenSessions() {
        return billingService.getOpenSessions();
    }
}