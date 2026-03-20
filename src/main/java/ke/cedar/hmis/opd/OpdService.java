package ke.cedar.hmis.opd;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.reception.Patient;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class OpdService {

    @Transactional
    public Visit checkInPatient(Long patientId, String chiefComplaint) {
        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }
        long todayCount = Visit.countTodaysQueue() + 1;
        String visitNo  = String.format("V%s-%03d",
            LocalDate.now().toString().replace("-", ""), todayCount);

        Visit visit          = new Visit();
        visit.patient        = patient;
        visit.visitNo        = visitNo;
        visit.queueNumber    = (int) todayCount;
        visit.chiefComplaint = chiefComplaint;
        visit.status         = "WAITING";
        visit.visitType      = "OPD";
        visit.persist();
        return visit;
    }

    @Transactional
    public Triage recordTriage(Long visitId, Triage triageData) {
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
        return triageData;
    }

    @Transactional
    public Visit updateVisitStatus(Long visitId,
            String status, String doctor) {
        Visit visit = Visit.findById(visitId);
        if (visit == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Visit not found\"}")
                    .build());
        }
        visit.status = status;
        if (doctor != null) visit.assignedDoctor = doctor;
        visit.persist();
        return visit;
    }

    @Transactional
    public Visit recordConsultation(Long visitId,
            String diagnosis, String notes) {
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
        return visit;
    }

    public List<Visit> getTodaysQueue() {
        return Visit.findTodaysQueue();
    }

    public List<Visit> getQueueByStatus(String status) {
        return Visit.findByStatus(status);
    }
}