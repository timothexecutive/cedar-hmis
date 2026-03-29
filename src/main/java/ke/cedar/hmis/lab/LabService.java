package ke.cedar.hmis.lab;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import java.util.List;

@ApplicationScoped
public class LabService {

    @Transactional
    public LabRequest createRequest(Long patientId,
            Long visitId, String requestedBy,
            String priority, String notes,
            List<Long> testIds,
            Long userId, String userName) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        long count = LabRequest.count() + 1;
        String reqNo = String.format("LAB-%s-%04d",
            java.time.LocalDate.now().toString()
                .replace("-",""), count);

        LabRequest request    = new LabRequest();
        request.patient       = patient;
        request.requestNo     = reqNo;
        request.requestedBy   = userName;
        request.priority      = priority != null ?
                priority : "ROUTINE";
        request.notes         = notes;
        request.status        = "PENDING";

        if (visitId != null) {
            Visit visit = Visit.findById(visitId);
            if (visit != null) request.visit = visit;
        }

        request.persist();

        for (Long testId : testIds) {
            LabTest test = LabTest.findById(testId);
            if (test == null) continue;

            LabResult result    = new LabResult();
            result.request      = request;
            result.test         = test;
            result.status       = "PENDING";
            result.persist();
        }

        AuditTrail.log(
            userId, userName, "DOCTOR",
            "LAB_REQUEST_CREATED", "LAB",
            "LabRequest", String.valueOf(request.id),
            "Lab request: " + reqNo +
            " | Patient: " + patient.fullName +
            " | Tests: " + testIds.size() +
            " | Priority: " + request.priority +
            " | By: " + userName);

        return request;
    }

    @Transactional
    public LabResult recordResult(Long resultId,
            String resultValue, String unit,
            String referenceRange, String flag,
            String notes, String doneBy,
            Long userId, String userName) {

        LabResult result = LabResult.findById(resultId);
        if (result == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Result not found\"}")
                    .build());
        }

        result.resultValue    = resultValue;
        result.unit           = unit;
        result.referenceRange = referenceRange;
        result.flag           = flag != null ? flag : "NORMAL";
        result.notes          = notes;
        result.doneBy         = userName;
        result.status         = "DONE";
        // doneAt is not a field on LabResult; updatedAt is set via @PreUpdate
        result.persist();

        AuditTrail.log(
            userId, userName, "LAB_TECH",
            "LAB_RESULT_RECORDED", "LAB",
            "LabResult", String.valueOf(resultId),
            "Result recorded: " +
            result.test.name +
            " | Patient: " + result.request.patient.fullName +
            " | Value: " + resultValue +
            " | Flag: " + result.flag +
            " | By: " + userName);

        return result;
    }

    @Transactional
    public LabResult verifyResult(Long resultId,
            String verifiedBy,
            Long userId, String userName) {

        LabResult result = LabResult.findById(resultId);
        if (result == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Result not found\"}")
                    .build());
        }

        if (!result.status.equals("DONE")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Result must be " +
                            "recorded before verification\"}")
                    .build());
        }

        result.verifiedBy = userName;
        result.status     = "VERIFIED";
        // verifiedAt is not a field on LabResult; updatedAt is set via @PreUpdate
        result.persist();

        AuditTrail.log(
            userId, userName, "LAB_TECH",
            "RESULT_VERIFIED", "LAB",
            "LabResult", String.valueOf(resultId),
            "Result verified: " +
            result.test.name +
            " | Patient: " + result.request.patient.fullName +
            " | Value: " + result.resultValue +
            " | Flag: " + result.flag +
            " | By: " + userName);

        return result;
    }

    public List<LabTest> getAllTests() {
        return LabTest.listAll();
    }

    public List<LabRequest> getPendingRequests() {
        return LabRequest.findPending();
    }

    public List<LabRequest> getPatientRequests(
            Long patientId) {
        return LabRequest.findByPatient(patientId);
    }

    public List<LabResult> getResults(Long requestId) {
        return LabResult.findByRequest(requestId);
    }
}