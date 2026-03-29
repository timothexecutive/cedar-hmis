package ke.cedar.hmis.lab;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/lab")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LabResource {

    @Inject LabService   labService;
    @Inject JsonWebToken jwt;

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Cedar HMIS — Laboratory module is alive!";
    }

    @GET
    @Path("/tests")
    @RolesAllowed({"DOCTOR","NURSE","LAB_TECH",
                   "CLINICAL_OFFICER","ADMIN"})
    public List<LabTest> getAllTests() {
        return labService.getAllTests();
    }

    @POST
    @Path("/requests")
    @Transactional
    @RolesAllowed({"DOCTOR","NURSE","CLINICAL_OFFICER",
                   "ADMIN"})
    public Response createRequest(
            Map<String, Object> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        Long patientId  = Long.parseLong(
            body.get("patientId").toString());
        Long visitId    = body.get("visitId") != null ?
            Long.parseLong(
                body.get("visitId").toString()) : null;
        String priority = (String) body.get("priority");
        String notes    = (String) body.get("notes");

        @SuppressWarnings("unchecked")
        List<Integer> rawIds =
            (List<Integer>) body.get("testIds");
        List<Long> testIds = rawIds.stream()
            .map(Long::valueOf)
            .collect(Collectors.toList());

        LabRequest request = labService.createRequest(
            patientId, visitId, userName,
            priority, notes, testIds,
            userId, userName);
        return Response.status(201)
            .entity(request).build();
    }

    @GET
    @Path("/requests/pending")
    @RolesAllowed({"LAB_TECH","ADMIN"})
    public List<LabRequest> getPending() {
        return labService.getPendingRequests();
    }

    @GET
    @Path("/requests/patient/{patientId}")
    @RolesAllowed({"DOCTOR","NURSE","LAB_TECH",
                   "CLINICAL_OFFICER","ADMIN"})
    public List<LabRequest> getPatientRequests(
            @PathParam("patientId") Long patientId) {
        return labService.getPatientRequests(patientId);
    }

    @GET
    @Path("/requests/{requestId}/results")
    @RolesAllowed({"DOCTOR","NURSE","LAB_TECH",
                   "CLINICAL_OFFICER","ADMIN"})
    public List<LabResult> getResults(
            @PathParam("requestId") Long requestId) {
        return labService.getResults(requestId);
    }

    @PUT
    @Path("/results/{resultId}")
    @Transactional
    @RolesAllowed({"LAB_TECH","ADMIN"})
    public LabResult recordResult(
            @PathParam("resultId") Long resultId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        return labService.recordResult(
            resultId,
            body.get("resultValue"),
            body.get("unit"),
            body.get("referenceRange"),
            body.get("flag"),
            body.get("notes"),
            userName,
            userId, userName);
    }

    @PUT
    @Path("/results/{resultId}/verify")
    @Transactional
    @RolesAllowed({"LAB_TECH","ADMIN"})
    public LabResult verifyResult(
            @PathParam("resultId") Long resultId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        return labService.verifyResult(
            resultId, userName, userId, userName);
    }
}