package ke.cedar.hmis.billing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import ke.cedar.hmis.ipd.Admission;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class BillingService {

    @Transactional
    public Invoice createInvoice(Long patientId, Long visitId,
            Long admissionId, String invoiceType,
            String createdBy, List<Map<String, Object>> items) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        String invNo = "INV-" +
            LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                Invoice.count() + 1);

        Invoice invoice       = new Invoice();
        invoice.invoiceNo     = invNo;
        invoice.patient       = patient;
        invoice.invoiceType   = invoiceType != null ? invoiceType : "OPD";
        invoice.createdBy     = createdBy;
        invoice.status        = "PENDING";

        if (visitId != null) {
            Visit visit = Visit.findById(visitId);
            if (visit != null) invoice.visit = visit;
        }

        if (admissionId != null) {
            Admission admission = Admission.findById(admissionId);
            if (admission != null) invoice.admission = admission;
        }

        invoice.persist();

        // Add line items and calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            InvoiceItem invItem  = new InvoiceItem();
            invItem.invoice      = invoice;
            invItem.description  = (String) item.get("description");
            invItem.category     = (String) item.get("category");
            invItem.quantity     = Integer.parseInt(
                item.get("quantity").toString());
            invItem.unitPrice    = new BigDecimal(
                item.get("unitPrice").toString());
            invItem.total        = invItem.unitPrice.multiply(
                BigDecimal.valueOf(invItem.quantity));
            invItem.persist();
            subtotal = subtotal.add(invItem.total);
        }

        invoice.subtotal      = subtotal;
        invoice.totalAmount   = subtotal;
        invoice.patientAmount = subtotal;
        invoice.balance       = subtotal;
        invoice.persist();

        return invoice;
    }

    @Transactional
    public Payment recordPayment(Long invoiceId,
            String method, BigDecimal amount,
            String referenceNo, String cashier) {

        Invoice invoice = Invoice.findById(invoiceId);
        if (invoice == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Invoice not found\"}")
                    .build());
        }

        Payment payment       = new Payment();
        payment.invoice       = invoice;
        payment.paymentMethod = method;
        payment.amount        = amount;
        payment.referenceNo   = referenceNo;
        payment.cashier       = cashier;
        payment.verified      = method.equals("CASH");
        payment.persist();

        // Update invoice totals — balance cannot go below zero
        invoice.paidAmount = invoice.paidAmount.add(amount);
        invoice.balance    = invoice.totalAmount
            .subtract(invoice.paidAmount)
            .max(BigDecimal.ZERO);

        invoice.status = invoice.balance
            .compareTo(BigDecimal.ZERO) <= 0 ? "PAID" : "PARTIAL";
        invoice.persist();

        return payment;
    }

    @Transactional
    public Payment initiateMpesaPayment(Long invoiceId,
            String phone, String cashier) {

        Invoice invoice = Invoice.findById(invoiceId);
        if (invoice == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Invoice not found\"}")
                    .build());
        }

        // ── FRAUD CHECK: block payment on already paid invoice ──
        if (invoice.status.equals("PAID")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Invoice is already fully paid\"}")
                    .build());
        }

        Payment payment       = new Payment();
        payment.invoice       = invoice;
        payment.paymentMethod = "MPESA";
        payment.amount        = invoice.balance;
        payment.phoneNumber   = phone;
        payment.cashier       = cashier;
        payment.verified      = false;
        payment.persist();

        return payment;
    }

    @Transactional
    public void verifyMpesaPayment(String checkoutRequestId,
            String mpesaReceipt, BigDecimal amount) {

        Payment payment = Payment.find(
            "referenceNo", checkoutRequestId).firstResult();

        if (payment == null) return;

        payment.mpesaReceipt = mpesaReceipt;
        payment.verified     = true;
        payment.persist();

        // Update invoice — balance cannot go below zero
        Invoice invoice    = payment.invoice;
        invoice.paidAmount = invoice.paidAmount.add(payment.amount);
        invoice.balance    = invoice.totalAmount
            .subtract(invoice.paidAmount)
            .max(BigDecimal.ZERO);
        invoice.status     = invoice.balance
            .compareTo(BigDecimal.ZERO) <= 0 ? "PAID" : "PARTIAL";
        invoice.persist();
    }

    @Transactional
    public InsuranceClaim submitClaim(Long invoiceId,
            Long providerId, String memberNo,
            BigDecimal amount) {

        Invoice invoice = Invoice.findById(invoiceId);
        if (invoice == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Invoice not found\"}")
                    .build());
        }

        InsuranceProvider provider =
            InsuranceProvider.findById(providerId);
        if (provider == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Insurance provider not found\"}")
                    .build());
        }

        // ── FRAUD CHECK: block duplicate claims on same invoice ──
        long existingClaims = InsuranceClaim.count(
            "invoice.id = ?1 AND status != 'REJECTED'", invoiceId);
        if (existingClaims > 0) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"A claim already exists for this invoice\"}")
                    .build());
        }

        // ── FRAUD CHECK: claimed amount cannot exceed invoice total ──
        if (amount.compareTo(invoice.totalAmount) > 0) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Claimed amount exceeds invoice total\"}")
                    .build());
        }

        // ── FRAUD CHECK: cannot claim on fully cash-paid invoice ──
        if (invoice.status.equals("PAID") &&
                invoice.paidAmount.compareTo(invoice.totalAmount) >= 0 &&
                invoice.insuranceAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Invoice already fully paid in cash\"}")
                    .build());
        }

        String claimNo = "CLM-" +
            LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                InsuranceClaim.count() + 1);

        InsuranceClaim claim   = new InsuranceClaim();
        claim.claimNo          = claimNo;
        claim.invoice          = invoice;
        claim.provider         = provider;
        claim.patient          = invoice.patient;
        claim.memberNo         = memberNo;
        claim.amountClaimed    = amount;
        claim.status           = "SUBMITTED";
        claim.persist();

        // Update invoice insurance amount
        invoice.insuranceAmount = amount;
        invoice.patientAmount   = invoice.totalAmount.subtract(amount);
        invoice.balance         = invoice.patientAmount
            .subtract(invoice.paidAmount)
            .max(BigDecimal.ZERO);
        invoice.persist();

        return claim;
    }

    @Transactional
    public InsuranceClaim updateClaimStatus(Long claimId,
            String status, BigDecimal approvedAmount,
            String refNo, String rejectionReason) {

        InsuranceClaim claim = InsuranceClaim.findById(claimId);
        if (claim == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Claim not found\"}")
                    .build());
        }

        // ── FRAUD CHECK: approved amount cannot exceed claimed amount ──
        if (approvedAmount != null &&
                approvedAmount.compareTo(claim.amountClaimed) > 0) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Approved amount exceeds claimed amount\"}")
                    .build());
        }

        claim.status = status;
        if (approvedAmount != null) {
            claim.amountApproved = approvedAmount;
        }
        if (refNo != null) claim.claimRefNo = refNo;
        if (rejectionReason != null) {
            claim.rejectionReason = rejectionReason;
        }
        if (status.equals("APPROVED")) {
            claim.approvalDate = LocalDateTime.now();
        }
        if (status.equals("PAID")) {
            claim.paymentDate = LocalDateTime.now();
            claim.amountPaid  = claim.amountApproved;
        }
        claim.persist();
        return claim;
    }

    public List<Invoice>           getPatientInvoices(Long p)  { return Invoice.findByPatient(p); }
    public List<Invoice>           getPendingInvoices()         { return Invoice.findPending(); }
    public List<InvoiceItem>       getInvoiceItems(Long invId)  { return InvoiceItem.findByInvoice(invId); }
    public List<Payment>           getPayments(Long invId)      { return Payment.findByInvoice(invId); }
    public List<InsuranceProvider> getProviders()               { return InsuranceProvider.findAllActive(); }
    public List<InsuranceClaim>    getClaimsByStatus(String s)  { return InsuranceClaim.findByStatus(s); }
    public List<InsuranceClaim>    getClaimsByProvider(Long p)  { return InsuranceClaim.findByProvider(p); }
}