package ke.cedar.hmis.billing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
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
    public Invoice createInvoice(Long patientId,
            Long visitId, Long admissionId,
            String invoiceType, String createdBy,
            List<Map<String, Object>> items,
            Long userId, String userName) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        String invNo = "INV-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                Invoice.count() + 1);

        Invoice invoice     = new Invoice();
        invoice.invoiceNo   = invNo;
        invoice.patient     = patient;
        invoice.invoiceType = invoiceType != null ?
                invoiceType : "OPD";
        invoice.createdBy   = userName;
        invoice.status      = "PENDING";

        if (visitId != null) {
            Visit visit = Visit.findById(visitId);
            if (visit != null) invoice.visit = visit;
        }
        if (admissionId != null) {
            Admission adm = Admission.findById(admissionId);
            if (adm != null) invoice.admission = adm;
        }

        invoice.persist();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            InvoiceItem inv = new InvoiceItem();
            inv.invoice     = invoice;
            inv.description =
                (String) item.get("description");
            inv.category    =
                (String) item.get("category");
            inv.quantity    = Integer.parseInt(
                item.get("quantity").toString());
            inv.unitPrice   = new BigDecimal(
                item.get("unitPrice").toString());
            inv.total       = inv.unitPrice.multiply(
                BigDecimal.valueOf(inv.quantity));
            inv.persist();
            subtotal = subtotal.add(inv.total);
        }

        invoice.subtotal      = subtotal;
        invoice.totalAmount   = subtotal;
        invoice.patientAmount = subtotal;
        invoice.balance       = subtotal;
        invoice.persist();

        AuditTrail.log(
            userId, userName, "CASHIER",
            "INVOICE_CREATED", "BILLING",
            "Invoice", String.valueOf(invoice.id),
            "Invoice created: " + invoice.invoiceNo +
            " | Patient: " + patient.fullName +
            " | Total: KES " + invoice.totalAmount +
            " | Type: " + invoice.invoiceType +
            " | By: " + userName);

        return invoice;
    }

    @Transactional
    public Payment recordPayment(Long invoiceId,
            String method, BigDecimal amount,
            String referenceNo, String cashier,
            Long userId, String userName) {

        Invoice invoice = Invoice.findById(invoiceId);
        if (invoice == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Invoice not found\"}")
                    .build());
        }
        if (invoice.status.equals("VOID")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Cannot pay a " +
                            "voided invoice\"}")
                    .build());
        }
        if (invoice.status.equals("PAID")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Invoice is already " +
                            "fully paid\"}")
                    .build());
        }

        String receiptNo = "RCP-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                Payment.count() + 1);

        Payment payment       = new Payment();
        payment.invoice       = invoice;
        payment.paymentMethod = method;
        payment.amount        = amount;
        payment.referenceNo   = referenceNo;
        payment.receiptNo     = receiptNo;
        payment.cashier       = userName;
        payment.verified      = method.equals("CASH");
        payment.persist();

        invoice.paidAmount = invoice.paidAmount.add(amount);
        invoice.balance    = invoice.totalAmount
            .subtract(invoice.paidAmount)
            .max(BigDecimal.ZERO);
        invoice.status     = invoice.balance
            .compareTo(BigDecimal.ZERO) <= 0 ?
            "PAID" : "PARTIAL";
        invoice.persist();

        AuditTrail.log(
            userId, userName, "CASHIER",
            "PAYMENT_RECORDED", "BILLING",
            "Invoice", String.valueOf(invoiceId),
            "Payment of KES " + amount +
            " via " + method +
            " | Receipt: " + receiptNo +
            " | Invoice: " + invoice.invoiceNo +
            " | Patient: " + invoice.patient.fullName +
            " | Status: " + invoice.status +
            " | By: " + userName);

        return payment;
    }

    @Transactional
    public Payment initiateMpesaPayment(Long invoiceId,
            String phone, String cashier,
            Long userId, String userName) {

        Invoice invoice = Invoice.findById(invoiceId);
        if (invoice == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Invoice not found\"}")
                    .build());
        }
        if (invoice.status.equals("PAID")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Invoice already paid\"}")
                    .build());
        }
        if (invoice.status.equals("VOID")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Cannot pay voided " +
                            "invoice\"}")
                    .build());
        }

        long pendingMpesa = Payment.count(
            "invoice.id = ?1 AND paymentMethod = 'MPESA' " +
            "AND verified = false", invoiceId);
        if (pendingMpesa > 0) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"A pending M-Pesa " +
                            "payment already exists\"}")
                    .build());
        }

        String receiptNo = "RCP-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                Payment.count() + 1);

        Payment payment       = new Payment();
        payment.invoice       = invoice;
        payment.paymentMethod = "MPESA";
        payment.amount        = invoice.balance;
        payment.phoneNumber   = phone;
        payment.cashier       = userName;
        payment.receiptNo     = receiptNo;
        payment.verified      = false;
        payment.persist();

        AuditTrail.log(
            userId, userName, "CASHIER",
            "MPESA_INITIATED", "BILLING",
            "Invoice", String.valueOf(invoiceId),
            "M-Pesa STK push: " + invoice.invoiceNo +
            " | Amount: KES " + invoice.balance +
            " | Phone: " + phone +
            " | By: " + userName);

        return payment;
    }

    @Transactional
    public void verifyMpesaPayment(
            String checkoutRequestId,
            String mpesaReceipt, BigDecimal amount) {

        Payment payment = Payment.find(
            "referenceNo", checkoutRequestId)
            .firstResult();
        if (payment == null) return;

        payment.mpesaReceipt = mpesaReceipt;
        payment.verified     = true;
        payment.persist();

        Invoice invoice    = payment.invoice;
        invoice.paidAmount =
            invoice.paidAmount.add(payment.amount);
        invoice.balance    = invoice.totalAmount
            .subtract(invoice.paidAmount)
            .max(BigDecimal.ZERO);
        invoice.status     = invoice.balance
            .compareTo(BigDecimal.ZERO) <= 0 ?
            "PAID" : "PARTIAL";
        invoice.persist();

        AuditTrail.log(
            null, "Safaricom", "SYSTEM",
            "MPESA_CONFIRMED", "BILLING",
            "Invoice", String.valueOf(invoice.id),
            "M-Pesa confirmed: " + invoice.invoiceNo +
            " | Receipt: " + mpesaReceipt +
            " | Amount: KES " + payment.amount +
            " | Status: " + invoice.status);
    }

    @Transactional
    public Invoice voidInvoice(Long invoiceId,
            String reason, String voidedBy,
            Long userId, String userName) {

        Invoice invoice = Invoice.findById(invoiceId);
        if (invoice == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Invoice not found\"}")
                    .build());
        }
        if (invoice.status.equals("PAID")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Cannot void a paid " +
                            "invoice. Issue a refund instead.\"}")
                    .build());
        }
        if (invoice.status.equals("VOID")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Invoice already " +
                            "voided\"}")
                    .build());
        }
        if (reason == null || reason.isBlank()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Void reason is " +
                            "mandatory\"}")
                    .build());
        }

        String oldStatus = invoice.status;
        invoice.status   = "VOID";
        invoice.notes    = "VOIDED by " + userName +
                           " | Reason: " + reason +
                           " | Time: " + LocalDateTime.now();
        invoice.persist();

        AuditTrail.logChange(
            userId, userName, "ADMIN",
            "INVOICE_VOIDED", "BILLING",
            "Invoice", String.valueOf(invoiceId),
            oldStatus, "VOID",
            "Invoice VOIDED: " + invoice.invoiceNo +
            " | Patient: " + invoice.patient.fullName +
            " | Amount: KES " + invoice.totalAmount +
            " | Reason: " + reason +
            " | By: " + userName);

        return invoice;
    }

    @Transactional
    public CashierSession openSession(String cashier,
            BigDecimal openingFloat,
            Long userId, String userName) {

        long open = CashierSession.count(
            "cashier = ?1 AND status = 'OPEN'", userName);
        if (open > 0) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Cashier already has " +
                            "an open session\"}")
                    .build());
        }

        CashierSession session = new CashierSession();
        session.cashier        = userName;
        session.openingFloat   = openingFloat;
        session.status         = "OPEN";
        session.persist();

        AuditTrail.log(
            userId, userName, "CASHIER",
            "SESSION_OPENED", "BILLING",
            "CashierSession", String.valueOf(session.id),
            "Session opened by: " + userName +
            " | Float: KES " + openingFloat);

        return session;
    }

    @Transactional
    public CashierSession closeSession(Long sessionId,
            BigDecimal actualCash,
            Long userId, String userName) {

        CashierSession session =
            CashierSession.findById(sessionId);
        if (session == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Session not found\"}")
                    .build());
        }
        if (session.status.equals("CLOSED")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Session already " +
                            "closed\"}")
                    .build());
        }

        BigDecimal expectedCash = Payment.find(
            "paymentMethod = 'CASH' AND verified = true " +
            "AND createdAt >= ?1", session.openedAt)
            .stream()
            .map(p -> ((Payment) p).amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        session.expectedCash =
            expectedCash.add(session.openingFloat);
        session.actualCash   = actualCash;
        session.variance     =
            actualCash.subtract(session.expectedCash);
        session.status       = "CLOSED";
        session.closedAt     = LocalDateTime.now();
        session.persist();

        AuditTrail.log(
            userId, userName, "CASHIER",
            "SESSION_CLOSED", "BILLING",
            "CashierSession", String.valueOf(sessionId),
            "Session closed by: " + userName +
            " | Expected: KES " + session.expectedCash +
            " | Actual: KES " + actualCash +
            " | Variance: KES " + session.variance +
            (session.variance.compareTo(
                BigDecimal.ZERO) != 0
                ? " ⚠ VARIANCE" : " ✓ Balanced"));

        return session;
    }

    @Transactional
    public InsuranceClaim submitClaim(Long invoiceId,
            Long providerId, String memberNo,
            BigDecimal amount,
            Long userId, String userName) {

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
                    .entity("{\"error\":\"Provider not found\"}")
                    .build());
        }
        long existing = InsuranceClaim.count(
            "invoice.id = ?1 AND status != 'REJECTED'",
            invoiceId);
        if (existing > 0) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Claim already " +
                            "exists for this invoice\"}")
                    .build());
        }
        if (amount.compareTo(invoice.totalAmount) > 0) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Claimed amount " +
                            "exceeds invoice total\"}")
                    .build());
        }

        String claimNo = "CLM-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                InsuranceClaim.count() + 1);

        InsuranceClaim claim = new InsuranceClaim();
        claim.claimNo        = claimNo;
        claim.invoice        = invoice;
        claim.provider       = provider;
        claim.patient        = invoice.patient;
        claim.memberNo       = memberNo;
        claim.amountClaimed  = amount;
        claim.status         = "SUBMITTED";
        claim.persist();

        invoice.insuranceAmount = amount;
        invoice.patientAmount   =
            invoice.totalAmount.subtract(amount);
        invoice.balance         = invoice.patientAmount
            .subtract(invoice.paidAmount)
            .max(BigDecimal.ZERO);
        invoice.persist();

        AuditTrail.log(
            userId, userName, "CASHIER",
            "CLAIM_SUBMITTED", "BILLING",
            "Invoice", String.valueOf(invoiceId),
            "Claim submitted: " + claimNo +
            " | Provider: " + provider.name +
            " | Patient: " + invoice.patient.fullName +
            " | Amount: KES " + amount +
            " | Member: " + memberNo +
            " | By: " + userName);

        return claim;
    }

    @Transactional
    public InsuranceClaim updateClaimStatus(Long claimId,
            String status, BigDecimal approvedAmount,
            String refNo, String rejectionReason,
            Long userId, String userName) {

        InsuranceClaim claim =
            InsuranceClaim.findById(claimId);
        if (claim == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Claim not found\"}")
                    .build());
        }
        if (approvedAmount != null &&
                approvedAmount.compareTo(
                    claim.amountClaimed) > 0) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Approved amount " +
                            "exceeds claimed amount\"}")
                    .build());
        }

        String oldStatus  = claim.status;
        claim.status      = status;
        if (approvedAmount != null)
            claim.amountApproved  = approvedAmount;
        if (refNo != null)
            claim.claimRefNo      = refNo;
        if (rejectionReason != null)
            claim.rejectionReason = rejectionReason;
        if (status.equals("APPROVED"))
            claim.approvalDate    = LocalDateTime.now();
        if (status.equals("PAID")) {
            claim.paymentDate = LocalDateTime.now();
            claim.amountPaid  = claim.amountApproved;
        }
        claim.persist();

        AuditTrail.logChange(
            userId, userName, "CASHIER",
            "CLAIM_STATUS_UPDATED", "BILLING",
            "InsuranceClaim", String.valueOf(claimId),
            oldStatus, status,
            "Claim " + claim.claimNo +
            " updated from " + oldStatus +
            " to " + status +
            (refNo != null ? " | Ref: " + refNo : "") +
            " | By: " + userName);

        return claim;
    }

    public List<Invoice> getPatientInvoices(Long p) {
        return Invoice.findByPatient(p); }
    public List<Invoice> getPendingInvoices() {
        return Invoice.findPending(); }
    public List<InvoiceItem> getInvoiceItems(Long id) {
        return InvoiceItem.findByInvoice(id); }
    public List<Payment> getPayments(Long id) {
        return Payment.findByInvoice(id); }
    public List<InsuranceProvider> getProviders() {
        return InsuranceProvider.findAllActive(); }
    public List<InsuranceClaim> getClaimsByStatus(String s) {
        return InsuranceClaim.findByStatus(s); }
    public List<InsuranceClaim> getClaimsByProvider(Long p) {
        return InsuranceClaim.findByProvider(p); }
    public List<CashierSession> getOpenSessions() {
        return CashierSession.findOpen(); }
}