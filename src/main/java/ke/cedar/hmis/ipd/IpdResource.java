package ke.cedar.hmis.ipd;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

@Path("/api/ipd")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IpdResource {

    @Inject IpdService   ipdService;
    @Inject JsonWebToken jwt;

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Cedar HMIS — IPD module is alive!";
    }

    @GET
    @Path("/wards")
    @RolesAllowed({"RECEPTIONIST","NURSE","DOCTOR",
                   "ADMIN","CEO","CLINICAL_OFFICER"})
    public List<Ward> getAllWards() {
        return ipdService.getAllWards();
    }

    @GET
    @Path("/wards/{wardId}/beds")
    @RolesAllowed({"RECEPTIONIST","NURSE","DOCTOR",
                   "ADMIN","CEO","CLINICAL_OFFICER"})
    public List<Bed> getBedsByWard(
            @PathParam("wardId") Long wardId) {
        return ipdService.getBedsByWard(wardId);
    }

    @GET
    @Path("/wards/{wardId}/beds/available")
    @RolesAllowed({"RECEPTIONIST","NURSE","DOCTOR","ADMIN"})
    public List<Bed> getAvailableBeds(
            @PathParam("wardId") Long wardId) {
        return ipdService.getAvailableBeds(wardId);
    }

    @PUT
    @Path("/beds/{bedId}/status")
    @Transactional
    @RolesAllowed({"NURSE","ADMIN"})
    public Bed updateBedStatus(
            @PathParam("bedId") Long bedId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        return ipdService.updateBedStatus(
            bedId, body.get("status"), userId, userName);
    }

    @POST
    @Path("/admit")
    @Transactional
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public Response admitPatient(Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        Admission admission = ipdService.admitPatient(
            Long.parseLong(body.get("patientId")),
            Long.parseLong(body.get("bedId")),
            body.get("doctor"),
            body.get("diagnosis"),
            userId, userName);
        return Response.status(201)
            .entity(admission).build();
    }

    @GET
    @Path("/admissions")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN","CEO"})
    public List<Admission> getAllAdmitted() {
        return ipdService.getAllAdmitted();
    }

    @GET
    @Path("/wards/{wardId}/admissions")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<Admission> getByWard(
            @PathParam("wardId") Long wardId) {
        return ipdService.getAdmissionsByWard(wardId);
    }

    @PUT
    @Path("/admissions/{admissionId}/discharge")
    @Transactional
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public Admission discharge(
            @PathParam("admissionId") Long admissionId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        return ipdService.dischargePatient(
            admissionId, body.get("dischargeSummary"),
            userId, userName);
    }

    @POST
    @Path("/admissions/{admissionId}/rounds")
    @Transactional
    @RolesAllowed({"DOCTOR","CLINICAL_OFFICER","ADMIN"})
    public Response addWardRound(
            @PathParam("admissionId") Long admissionId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        WardRound round = ipdService.addWardRound(
            admissionId,
            body.get("doctor"),
            body.get("notes"),
            body.get("plan"),
            userId, userName);
        return Response.status(201).entity(round).build();
    }

    @GET
    @Path("/admissions/{admissionId}/rounds")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<WardRound> getWardRounds(
            @PathParam("admissionId") Long admissionId) {
        return ipdService.getWardRounds(admissionId);
    }
}