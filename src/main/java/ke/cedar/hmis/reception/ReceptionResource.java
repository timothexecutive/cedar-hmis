package ke.cedar.hmis.reception;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/reception")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReceptionResource {

    @Inject
    ReceptionService receptionService;

    // ── PING ─────────────────────────────────────────
    @GET
    @Path("/ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Cedar HMIS — Reception module is alive!";
    }

    // ── REGISTER new patient ──────────────────────────
    @POST
    @Path("/patients")
    @Transactional
    public Response registerPatient(Patient patient) {
        Patient saved = receptionService.registerPatient(patient);
        return Response.status(201).entity(saved).build();
    }

    // ── SEARCH / list all patients ────────────────────
    @GET
    @Path("/patients")
    public List<Patient> searchPatients(@QueryParam("q") String query) {
        return receptionService.searchPatients(query);
    }

    // ── GET single patient by ID ──────────────────────
    @GET
    @Path("/patients/{id}")
    public Patient getPatient(@PathParam("id") Long id) {
        return receptionService.getPatient(id);
    }
}


