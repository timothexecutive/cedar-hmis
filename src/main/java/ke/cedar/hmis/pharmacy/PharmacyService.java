package ke.cedar.hmis.pharmacy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PharmacyService {

    @Transactional
    public Prescription createPrescription(Long patientId,
            Long visitId, String prescribedBy,
            String notes, List<Map<String, Object>> items) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        String rxNo = "RX-" +
            LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                Prescription.count() + 1);

        Prescription prescription    = new Prescription();
        prescription.prescriptionNo  = rxNo;
        prescription.patient         = patient;
        prescription.prescribedBy    = prescribedBy;
        prescription.notes           = notes;
        prescription.status          = "PENDING";

        if (visitId != null) {
            Visit visit = Visit.findById(visitId);
            if (visit != null) prescription.visit = visit;
        }

        prescription.persist();

        // Add each drug item
        for (Map<String, Object> item : items) {
            Long drugId = Long.parseLong(item.get("drugId").toString());
            Drug drug   = Drug.findById(drugId);
            if (drug == null) continue;

            PrescriptionItem rxItem   = new PrescriptionItem();
            rxItem.prescription       = prescription;
            rxItem.drug               = drug;
            rxItem.dosage             = (String) item.get("dosage");
            rxItem.frequency          = (String) item.get("frequency");
            rxItem.duration           = (String) item.get("duration");
            rxItem.quantity           = Integer.parseInt(
                item.get("quantity").toString());
            rxItem.instructions       = (String) item.get("instructions");
            rxItem.persist();
        }

        return prescription;
    }

    @Transactional
    public Dispensing dispenseDrug(Long prescriptionId,
            Long drugId, Integer quantity, String dispensedBy) {

        Prescription prescription = Prescription.findById(prescriptionId);
        if (prescription == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Prescription not found\"}")
                    .build());
        }

        Drug drug = Drug.findById(drugId);
        if (drug == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Drug not found\"}")
                    .build());
        }

        // Check stock
        if (drug.currentStock < quantity) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Insufficient stock. Available: "
                        + drug.currentStock + "\"}")
                    .build());
        }

        // Deduct stock
        drug.currentStock -= quantity;
        drug.persist();

        // Record dispensing
        Dispensing dispensing       = new Dispensing();
        dispensing.prescription     = prescription;
        dispensing.drug             = drug;
        dispensing.quantity         = quantity;
        dispensing.dispensedBy      = dispensedBy;
        dispensing.persist();

        // Update prescription item status
        List<PrescriptionItem> items =
            PrescriptionItem.findByPrescription(prescriptionId);
        for (PrescriptionItem item : items) {
            if (item.drug.id.equals(drugId)) {
                item.status = "DISPENSED";
                item.persist();
            }
        }

        // Check if all items dispensed
        boolean allDispensed = items.stream()
            .allMatch(i -> i.status.equals("DISPENSED"));
        prescription.status = allDispensed ? "DISPENSED" : "PARTIAL";
        prescription.persist();

        return dispensing;
    }

    @Transactional
    public Drug updateStock(Long drugId, Integer quantity, String type) {
        Drug drug = Drug.findById(drugId);
        if (drug == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Drug not found\"}")
                    .build());
        }
        if (type.equals("ADD")) {
            drug.currentStock += quantity;
        } else {
            if (drug.currentStock < quantity) {
                throw new WebApplicationException(
                    Response.status(409)
                        .entity("{\"error\":\"Insufficient stock\"}")
                        .build());
            }
            drug.currentStock -= quantity;
        }
        drug.persist();
        return drug;
    }

    public List<Drug>         getAllDrugs()                  { return Drug.findAllActive(); }
    public List<Drug>         getLowStock()                  { return Drug.findLowStock(); }
    public List<Drug>         searchDrugs(String q)          { return Drug.search(q); }
    public List<Prescription> getPending()                   { return Prescription.findPending(); }
    public List<Prescription> getPatientPrescriptions(Long p){ return Prescription.findByPatient(p); }
    public List<PrescriptionItem> getItems(Long rxId)        { return PrescriptionItem.findByPrescription(rxId); }
    public List<Dispensing>   getDispensing(Long rxId)       { return Dispensing.findByPrescription(rxId); }
}