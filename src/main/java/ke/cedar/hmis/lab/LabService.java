package ke.cedar.hmis.lab;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class LabService {

    @Transactional
    public LabRequest createRequest(Long patientId, Long visitId,
            String requestedBy, String priority,
            String notes, List<Long> testIds) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        String reqNo = "LAB-" +
            LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                LabRequest.count() + 1);

        LabRequest request    = new LabRequest();
        request.requestNo     = reqNo;
        request.patient       = patient;
        request.requestedBy   = requestedBy;
        request.priority      = priority != null ? priority : "ROUTINE";
        request.notes         = notes;
        request.status        = "PENDING";

        if (visitId != null) {
            Visit visit = Visit.findById(visitId);
            if (visit != null) request.visit = visit;
        }

        request.persist();

        // Create a result entry for each test ordered
        for (Long testId : testIds) {
            LabTest test = LabTest.findById(testId);
            if (test != null) {
                LabResult result = new LabResult();
                result.request  = request;
                result.test     = test;
                result.status   = "PENDING";
                result.persist();
            }
        }

        return request;
    }

    @Transactional
    public LabResult recordResult(Long resultId, String value,
            String unit, String referenceRange,
            String flag, String notes, String doneBy) {

        LabResult result = LabResult.findById(resultId);
        if (result == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Result not found\"}")
                    .build());
        }

        result.resultValue    = value;
        result.unit           = unit;
        result.referenceRange = referenceRange;
        result.flag           = flag != null ? flag : "NORMAL";
        result.notes          = notes;
        result.doneBy         = doneBy;
        result.status         = "RESULTED";
        result.persist();

        // Check if all results for this request are done
        List<LabResult> allResults =
            LabResult.findByRequest(result.request.id);
        boolean allDone = allResults.stream()
            .allMatch(r -> r.status.equals("RESULTED") ||
                          r.status.equals("VERIFIED"));

        if (allDone) {
            result.request.status = "COMPLETED";
            result.request.persist();
        } else {
            result.request.status = "IN_PROGRESS";
            result.request.persist();
        }

        return result;
    }

    @Transactional
    public LabResult verifyResult(Long resultId, String verifiedBy) {
        LabResult result = LabResult.findById(resultId);
        if (result == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Result not found\"}")
                    .build());
        }
        result.verifiedBy = verifiedBy;
        result.status     = "VERIFIED";
        result.persist();
        return result;
    }

    public List<LabTest>    getAllTests()              { return LabTest.findAllActive(); }
    public List<LabRequest> getPendingRequests()       { return LabRequest.findPending(); }
    public List<LabRequest> getPatientRequests(Long p) { return LabRequest.findByPatient(p); }
    public List<LabResult>  getResults(Long requestId) { return LabResult.findByRequest(requestId); }
}