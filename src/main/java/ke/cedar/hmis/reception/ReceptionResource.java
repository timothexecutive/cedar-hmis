package ke.cedar.hmis.reception;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;

@Path("/api/reception")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReceptionResource {

    @Inject ReceptionService receptionService;
    @Inject JsonWebToken     jwt;

    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Cedar HMIS — Reception module is alive!";
    }

    @POST
    @Path("/patients")
    @Transactional
    @RolesAllowed({"RECEPTIONIST","NURSE","DOCTOR","ADMIN"})
    public Response registerPatient(Patient patient) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        patient.registeredBy = userName;
        Patient saved = receptionService
            .registerPatient(patient, userId, userName);
        return Response.status(201).entity(saved).build();
    }

    @GET
    @Path("/patients")
    @RolesAllowed({"RECEPTIONIST","NURSE","DOCTOR","ADMIN",
                   "CASHIER","LAB_TECH","PHARMACIST"})
    public List<Patient> searchPatients(
            @QueryParam("q") String query) {
        return receptionService.searchPatients(query);
    }

    @GET
    @Path("/patients/{id}")
    @RolesAllowed({"RECEPTIONIST","NURSE","DOCTOR","ADMIN",
                   "CASHIER","LAB_TECH","PHARMACIST"})
    public Patient getPatient(@PathParam("id") Long id) {
        return receptionService.getPatient(id);
    }
}