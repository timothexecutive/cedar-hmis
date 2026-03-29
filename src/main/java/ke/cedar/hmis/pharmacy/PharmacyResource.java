package ke.cedar.hmis.pharmacy;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

@Path("/api/pharmacy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PharmacyResource {

    @Inject PharmacyService pharmacyService;
    @Inject JsonWebToken    jwt;

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Cedar HMIS — Pharmacy module is alive!";
    }

    @GET
    @Path("/drugs")
    @RolesAllowed({"PHARMACIST","DOCTOR","NURSE",
                   "CLINICAL_OFFICER","ADMIN"})
    public List<Drug> getAllDrugs() {
        return pharmacyService.getAllDrugs();
    }

    @GET
    @Path("/drugs/search")
    @RolesAllowed({"PHARMACIST","DOCTOR","NURSE",
                   "CLINICAL_OFFICER","ADMIN"})
    public List<Drug> searchDrugs(
            @QueryParam("q") String query) {
        return pharmacyService.searchDrugs(query);
    }

    @GET
    @Path("/drugs/low-stock")
    @RolesAllowed({"PHARMACIST","ADMIN"})
    public List<Drug> getLowStock() {
        return pharmacyService.getLowStock();
    }

    @PUT
    @Path("/drugs/{drugId}/stock")
    @Transactional
    @RolesAllowed({"PHARMACIST","ADMIN"})
    public Drug updateStock(
            @PathParam("drugId") Long drugId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        return pharmacyService.updateStock(
            drugId,
            Integer.parseInt(body.get("quantity")),
            body.get("type"),
            userId, userName);
    }

    @POST
    @Path("/prescriptions")
    @Transactional
    @RolesAllowed({"DOCTOR","CLINICAL_OFFICER","ADMIN"})
    public Response createPrescription(
            Map<String, Object> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        Long patientId  = Long.parseLong(
            body.get("patientId").toString());
        Long visitId    = body.get("visitId") != null ?
            Long.parseLong(
                body.get("visitId").toString()) : null;
        String notes    = (String) body.get("notes");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items =
            (List<Map<String, Object>>) body.get("items");

        Prescription rx = pharmacyService
            .createPrescription(
                patientId, visitId, userName,
                notes, items, userId, userName);
        return Response.status(201).entity(rx).build();
    }

    @GET
    @Path("/prescriptions/pending")
    @RolesAllowed({"PHARMACIST","ADMIN"})
    public List<Prescription> getPending() {
        return pharmacyService.getPending();
    }

    @GET
    @Path("/prescriptions/patient/{patientId}")
    @RolesAllowed({"PHARMACIST","DOCTOR","NURSE",
                   "CLINICAL_OFFICER","ADMIN"})
    public List<Prescription> getPatientPrescriptions(
            @PathParam("patientId") Long patientId) {
        return pharmacyService
            .getPatientPrescriptions(patientId);
    }

    @GET
    @Path("/prescriptions/{rxId}/items")
    @RolesAllowed({"PHARMACIST","DOCTOR","ADMIN"})
    public List<PrescriptionItem> getItems(
            @PathParam("rxId") Long rxId) {
        return pharmacyService.getItems(rxId);
    }

    @POST
    @Path("/prescriptions/{rxId}/dispense")
    @Transactional
    @RolesAllowed({"PHARMACIST","ADMIN"})
    public Response dispense(
            @PathParam("rxId") Long rxId,
            Map<String, String> body) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        Dispensing dispensing = pharmacyService.dispenseDrug(
            rxId,
            Long.parseLong(body.get("drugId")),
            Integer.parseInt(body.get("quantity")),
            userName,
            userId, userName);
        return Response.status(201)
            .entity(dispensing).build();
    }

    @GET
    @Path("/prescriptions/{rxId}/dispensing")
    @RolesAllowed({"PHARMACIST","DOCTOR","ADMIN"})
    public List<Dispensing> getDispensing(
            @PathParam("rxId") Long rxId) {
        return pharmacyService.getDispensing(rxId);
    }
}