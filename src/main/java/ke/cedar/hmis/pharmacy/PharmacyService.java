package ke.cedar.hmis.pharmacy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PharmacyService {

    @Transactional
    public Drug updateStock(Long drugId,
            Integer quantity, String type,
            Long userId, String userName) {

        Drug drug = Drug.findById(drugId);
        if (drug == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Drug not found\"}")
                    .build());
        }

        int oldStock = drug.currentStock;

        if ("ADD".equals(type)) {
            drug.currentStock += quantity;
        } else if ("DEDUCT".equals(type)) {
            if (drug.currentStock < quantity) {
                throw new WebApplicationException(
                    Response.status(409)
                        .entity("{\"error\":\"Insufficient " +
                                "stock\"}")
                        .build());
            }
            drug.currentStock -= quantity;
        } else {
            drug.currentStock = quantity;
        }

        drug.persist();

        AuditTrail.logChange(
            userId, userName, "PHARMACIST",
            "STOCK_UPDATED", "PHARMACY",
            "Drug", String.valueOf(drugId),
            String.valueOf(oldStock),
            String.valueOf(drug.currentStock),
            "Stock updated: " + drug.name +
            " | Type: " + type +
            " | Qty: " + quantity +
            " | New stock: " + drug.currentStock +
            " | By: " + userName);

        return drug;
    }

    @Transactional
    public Prescription createPrescription(
            Long patientId, Long visitId,
            String prescribedBy, String notes,
            List<Map<String, Object>> items,
            Long userId, String userName) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        long count = Prescription.count() + 1;
        String rxNo = String.format("RX-%s-%04d",
            java.time.LocalDate.now().toString()
                .replace("-",""), count);

        Prescription rx = new Prescription();
        rx.patient        = patient;
        rx.prescriptionNo = rxNo;
        rx.prescribedBy   = userName;
        rx.notes          = notes;
        rx.status         = "PENDING";

        if (visitId != null) {
            Visit visit = Visit.findById(visitId);
            if (visit != null) rx.visit = visit;
        }

        rx.persist();

        for (Map<String, Object> item : items) {
            PrescriptionItem pi = new PrescriptionItem();
            pi.prescription     = rx;
            pi.drug = Drug.findById(Long.parseLong(
                item.get("drugId").toString()));
            pi.dosage       = (String) item.get("dosage");
            pi.frequency    = (String) item.get("frequency");
            pi.duration     = (String) item.get("duration");
            pi.instructions = (String) item.get("instructions");
            pi.status       = "PENDING";
            pi.persist();
        }

        AuditTrail.log(
            userId, userName, "DOCTOR",
            "PRESCRIPTION_CREATED", "PHARMACY",
            "Prescription", String.valueOf(rx.id),
            "Prescription: " + rxNo +
            " | Patient: " + patient.fullName +
            " | Items: " + items.size() +
            " | By: " + userName);

        return rx;
    }

    @Transactional
    public Dispensing dispenseDrug(Long rxId,
            Long drugId, Integer quantity,
            String dispensedBy,
            Long userId, String userName) {

        Prescription rx = Prescription.findById(rxId);
        if (rx == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Prescription " +
                            "not found\"}")
                    .build());
        }

        Drug drug = Drug.findById(drugId);
        if (drug == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Drug not found\"}")
                    .build());
        }

        if (drug.currentStock < quantity) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Insufficient stock. " +
                            "Available: " +
                            drug.currentStock + "\"}")
                    .build());
        }

        drug.currentStock -= quantity;
        drug.persist();

        Dispensing dispensing   = new Dispensing();
        dispensing.prescription = rx;
        dispensing.drug         = drug;
        dispensing.quantity     = quantity;
        dispensing.dispensedBy  = userName;
        // patient, unitPrice, total are not fields on Dispensing entity.
        // Patient is accessible via dispensing.prescription.patient
        // Pricing fields should be added to Dispensing entity if needed.
        dispensing.persist();

        rx.status = "DISPENSED";
        rx.persist();

        AuditTrail.log(
            userId, userName, "PHARMACIST",
            "DRUG_DISPENSED", "PHARMACY",
            "Prescription", String.valueOf(rxId),
            "Dispensed: " + drug.name +
            " | Qty: " + quantity +
            " | Patient: " + rx.patient.fullName +
            " | Stock left: " + drug.currentStock +
            " | By: " + userName);

        return dispensing;
    }

    public List<Drug> getAllDrugs() {
        return Drug.listAll();
    }

    public List<Drug> searchDrugs(String query) {
        if (query == null || query.isBlank())
            return Drug.listAll();
        return Drug.search(query);
    }

    public List<Drug> getLowStock() {
        return Drug.findLowStock();
    }

    public List<Prescription> getPending() {
        return Prescription.findPending();
    }

    public List<Prescription> getPatientPrescriptions(
            Long patientId) {
        return Prescription.findByPatient(patientId);
    }

    public List<PrescriptionItem> getItems(Long rxId) {
        return PrescriptionItem.findByPrescription(rxId);
    }

    public List<Dispensing> getDispensing(Long rxId) {
        return Dispensing.findByPrescription(rxId);
    }
}