package ke.cedar.hmis.ipd;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
import ke.cedar.hmis.reception.Patient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class IpdService {

    @Transactional
    public Admission admitPatient(Long patientId,
            Long bedId, String doctor, String diagnosis,
            Long userId, String userName) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }
        Bed bed = Bed.findById(bedId);
        if (bed == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Bed not found\"}")
                    .build());
        }
        if (!bed.status.equals("AVAILABLE")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Bed is not available\"}")
                    .build());
        }

        String admNo = "ADM-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                Admission.findAllAdmitted().size() + 1);

        bed.status = "OCCUPIED";
        bed.persist();

        Admission admission          = new Admission();
        admission.admissionNo        = admNo;
        admission.patient            = patient;
        admission.bed                = bed;
        admission.ward               = bed.ward;
        admission.admittingDoctor    = doctor;
        admission.admissionDiagnosis = diagnosis;
        admission.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "PATIENT_ADMITTED", "IPD",
            "Admission", String.valueOf(admission.id),
            "Patient admitted: " + patient.fullName +
            " | AdmNo: " + admNo +
            " | Ward: " + bed.ward.name +
            " | Bed: " + bed.bedNumber +
            " | Doctor: " + doctor +
            " | Diagnosis: " + diagnosis +
            " | By: " + userName);

        return admission;
    }

    @Transactional
    public Admission dischargePatient(Long admissionId,
            String dischargeSummary,
            Long userId, String userName) {

        Admission admission =
            Admission.findById(admissionId);
        if (admission == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Admission not found\"}")
                    .build());
        }

        admission.bed.status = "CLEANING";
        admission.bed.persist();
        admission.status           = "DISCHARGED";
        admission.dischargeDate    = LocalDateTime.now();
        admission.dischargeSummary = dischargeSummary;
        admission.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "PATIENT_DISCHARGED", "IPD",
            "Admission", String.valueOf(admissionId),
            "Patient discharged: " +
            admission.patient.fullName +
            " | AdmNo: " + admission.admissionNo +
            " | Bed released: " +
            admission.bed.bedNumber +
            " | Summary: " + dischargeSummary +
            " | By: " + userName);

        return admission;
    }

    @Transactional
    public WardRound addWardRound(Long admissionId,
            String doctor, String notes, String plan,
            Long userId, String userName) {

        Admission admission =
            Admission.findById(admissionId);
        if (admission == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Admission not found\"}")
                    .build());
        }

        WardRound round = new WardRound();
        round.admission = admission;
        round.doctor    = doctor;
        round.notes     = notes;
        round.plan      = plan;
        round.persist();

        AuditTrail.log(
            userId, userName, "DOCTOR",
            "WARD_ROUND_ADDED", "IPD",
            "Admission", String.valueOf(admissionId),
            "Ward round by " + doctor +
            " for " + admission.patient.fullName +
            " | Plan: " + plan +
            " | By: " + userName);

        return round;
    }

    @Transactional
    public Bed updateBedStatus(Long bedId, String status,
            Long userId, String userName) {
        Bed bed = Bed.findById(bedId);
        if (bed == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Bed not found\"}")
                    .build());
        }
        String oldStatus = bed.status;
        bed.status = status;
        bed.persist();

        AuditTrail.logChange(
            userId, userName, "NURSE",
            "BED_STATUS_CHANGED", "IPD",
            "Bed", String.valueOf(bedId),
            oldStatus, status,
            "Bed " + bed.bedNumber +
            " in " + bed.ward.name +
            " changed from " + oldStatus +
            " to " + status +
            " | By: " + userName);

        return bed;
    }

    public List<Ward>      getAllWards()                 { return Ward.findAllActive(); }
    public List<Bed>       getBedsByWard(Long wardId)    { return Bed.findByWard(wardId); }
    public List<Bed>       getAvailableBeds(Long wardId) { return Bed.findAvailableByWard(wardId); }
    public List<Admission> getAllAdmitted()               { return Admission.findAllAdmitted(); }
    public List<Admission> getAdmissionsByWard(Long w)   { return Admission.findByWard(w); }
    public List<WardRound> getWardRounds(Long admId)     { return WardRound.findByAdmission(admId); }
}