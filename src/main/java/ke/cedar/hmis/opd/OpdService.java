package ke.cedar.hmis.opd;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
import ke.cedar.hmis.reception.Patient;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class OpdService {

    @Transactional
    public Visit checkInPatient(Long patientId,
            String chiefComplaint,
            Long userId, String userName) {
        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }
        long todayCount = Visit.countTodaysQueue() + 1;
        String visitNo  = String.format("V%s-%03d",
            LocalDate.now().toString().replace("-", ""),
            todayCount);

        Visit visit          = new Visit();
        visit.patient        = patient;
        visit.visitNo        = visitNo;
        visit.queueNumber    = (int) todayCount;
        visit.chiefComplaint = chiefComplaint;
        visit.status         = "WAITING";
        visit.visitType      = "OPD";
        visit.persist();

        AuditTrail.log(
            userId, userName, "RECEPTIONIST",
            "PATIENT_CHECKIN", "OPD",
            "Visit", String.valueOf(visit.id),
            "Patient checked in: " + patient.fullName +
            " | VisitNo: " + visitNo +
            " | Queue#: " + visit.queueNumber +
            " | Complaint: " + chiefComplaint +
            " | By: " + userName);

        return visit;
    }

    @Transactional
    public Triage recordTriage(Long visitId,
            Triage triageData,
            Long userId, String userName) {
        Visit visit = Visit.findById(visitId);
        if (visit == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Visit not found\"}")
                    .build());
        }
        triageData.calculateTriageCategory();
        triageData.visit = visit;
        triageData.persist();
        visit.status = "TRIAGE_DONE";
        visit.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "TRIAGE_RECORDED", "OPD",
            "Visit", String.valueOf(visitId),
            "Triage done for " + visit.patient.fullName +
            " | Category: " + triageData.triageCategory +
            " | Temp: " + triageData.temperature +
            " | BP: " + triageData.bloodPressure +
            " | SpO2: " + triageData.spo2 +
            " | By: " + userName);

        return triageData;
    }

    @Transactional
    public Visit updateVisitStatus(Long visitId,
            String status, String doctor,
            Long userId, String userName) {
        Visit visit = Visit.findById(visitId);
        if (visit == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Visit not found\"}")
                    .build());
        }
        String oldStatus = visit.status;
        visit.status = status;
        if (doctor != null) visit.assignedDoctor = doctor;
        visit.persist();

        AuditTrail.logChange(
            userId, userName, "NURSE",
            "VISIT_STATUS_CHANGED", "OPD",
            "Visit", String.valueOf(visitId),
            oldStatus, status,
            "Visit status changed for " +
            visit.patient.fullName +
            (doctor != null ?
                " | Assigned to: " + doctor : "") +
            " | By: " + userName);

        return visit;
    }

    @Transactional
    public Visit recordConsultation(Long visitId,
            String diagnosis, String notes,
            Long userId, String userName) {
        Visit visit = Visit.findById(visitId);
        if (visit == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Visit not found\"}")
                    .build());
        }
        visit.diagnosis = diagnosis;
        visit.notes     = notes;
        visit.status    = "DONE";
        visit.persist();

        AuditTrail.log(
            userId, userName, "DOCTOR",
            "CONSULTATION_DONE", "OPD",
            "Visit", String.valueOf(visitId),
            "Consultation completed for " +
            visit.patient.fullName +
            " | Doctor: " + visit.assignedDoctor +
            " | Diagnosis: " + diagnosis +
            " | By: " + userName);

        return visit;
    }

    public List<Visit> getTodaysQueue() {
        return Visit.findTodaysQueue();
    }

    public List<Visit> getQueueByStatus(String status) {
        return Visit.findByStatus(status);
    }
}