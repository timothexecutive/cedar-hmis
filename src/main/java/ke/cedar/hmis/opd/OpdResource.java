package ke.cedar.hmis.opd;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

@Path("/api/opd")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OpdResource {

    @Inject OpdService   opdService;
    @Inject JsonWebToken jwt;

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Cedar HMIS — OPD module is alive!";
    }

    @POST
    @Path("/checkin")
    @Transactional
    @RolesAllowed({"RECEPTIONIST","NURSE","ADMIN"})
    public Response checkIn(Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        Visit visit = opdService.checkInPatient(
            Long.parseLong(body.get("patientId")),
            body.get("chiefComplaint"),
            userId, userName);
        return Response.status(201).entity(visit).build();
    }

    @GET
    @Path("/queue")
    @RolesAllowed({"RECEPTIONIST","NURSE","DOCTOR",
                   "CLINICAL_OFFICER","ADMIN","CEO"})
    public List<Visit> getTodaysQueue() {
        return opdService.getTodaysQueue();
    }

    @GET
    @Path("/queue/status/{status}")
    @RolesAllowed({"RECEPTIONIST","NURSE","DOCTOR",
                   "CLINICAL_OFFICER","ADMIN"})
    public List<Visit> getByStatus(
            @PathParam("status") String status) {
        return opdService.getQueueByStatus(status);
    }

    @POST
    @Path("/triage/{visitId}")
    @Transactional
    @RolesAllowed({"NURSE","CLINICAL_OFFICER","ADMIN"})
    public Response recordTriage(
            @PathParam("visitId") Long visitId,
            Triage triage) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        triage.doneBy   = userName;
        Triage saved = opdService.recordTriage(
            visitId, triage, userId, userName);
        return Response.status(201).entity(saved).build();
    }

    @PUT
    @Path("/visits/{visitId}/assign")
    @Transactional
    @RolesAllowed({"NURSE","ADMIN"})
    public Visit assignDoctor(
            @PathParam("visitId") Long visitId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        return opdService.updateVisitStatus(
            visitId, "WITH_DOCTOR",
            body.get("doctor"), userId, userName);
    }

    @PUT
    @Path("/visits/{visitId}/consultation")
    @Transactional
    @RolesAllowed({"DOCTOR","CLINICAL_OFFICER","ADMIN"})
    public Visit recordConsultation(
            @PathParam("visitId") Long visitId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        return opdService.recordConsultation(
            visitId, body.get("diagnosis"),
            body.get("notes"), userId, userName);
    }
}