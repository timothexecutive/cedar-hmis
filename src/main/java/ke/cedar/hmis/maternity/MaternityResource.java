package ke.cedar.hmis.maternity;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

@Path("/api/maternity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaternityResource {

    @Inject MaternityService maternityService;
    @Inject JsonWebToken     jwt;

    @POST
    @Path("/pregnancies/patient/{patientId}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public Response registerPregnancy(
            @PathParam("patientId") Long patientId,
            Pregnancy request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        request.registeredBy = userName;
        return Response.status(201)
            .entity(maternityService.registerPregnancy(
                patientId, request, userId, userName))
            .build();
    }

    @GET
    @Path("/pregnancies/active")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<Pregnancy> getActivePregnancies() {
        return maternityService.getActivePregnancies();
    }

    @GET
    @Path("/pregnancies/high-risk")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<Pregnancy> getHighRiskPregnancies() {
        return maternityService.getHighRiskPregnancies();
    }

    @GET
    @Path("/pregnancies/patient/{patientId}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "RECEPTIONIST","ADMIN"})
    public List<Pregnancy> getPatientPregnancies(
            @PathParam("patientId") Long patientId) {
        return maternityService
            .getPatientPregnancies(patientId);
    }

    @POST
    @Path("/anc/{pregnancyId}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public Response recordANCVisit(
            @PathParam("pregnancyId") Long pregnancyId,
            ANCVisit request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        request.seenBy  = userName;
        return Response.status(201)
            .entity(maternityService.recordANCVisit(
                pregnancyId, request, userId, userName))
            .build();
    }

    @GET
    @Path("/anc/{pregnancyId}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<ANCVisit> getANCVisits(
            @PathParam("pregnancyId") Long pregnancyId) {
        return maternityService.getANCVisits(pregnancyId);
    }

    @GET
    @Path("/anc/risk-flags")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<ANCVisit> getANCWithRiskFlags() {
        return maternityService.getANCWithRiskFlags();
    }

    @POST
    @Path("/admissions")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public Response admitForLabour(
            Map<String, Object> request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        Long patientId  = Long.parseLong(
            request.get("patientId").toString());
        Long pregnancyId =
            request.get("pregnancyId") != null
            ? Long.parseLong(
                request.get("pregnancyId").toString())
            : null;
        Long bedId = request.get("bedId") != null
            ? Long.parseLong(
                request.get("bedId").toString()) : null;
        Integer gestationWeeks =
            request.get("gestationWeeks") != null
            ? Integer.parseInt(
                request.get("gestationWeeks").toString())
            : null;

        return Response.status(201)
            .entity(maternityService.admitForLabour(
                patientId, pregnancyId, bedId,
                gestationWeeks,
                (String) request.get("membranesStatus"),
                (String) request.get("onsetOfLabour"),
                userName,
                (String) request.get("notes"),
                userId, userName))
            .build();
    }

    @GET
    @Path("/admissions/active")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<MaternityAdmission> getActiveLabour() {
        return maternityService.getActiveLabour();
    }

    @POST
    @Path("/deliveries/{maternityAdmissionId}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    @SuppressWarnings("unchecked")
    public Response recordDelivery(
            @PathParam("maternityAdmissionId")
            Long maternityAdmissionId,
            Map<String, Object> request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();

        Delivery delivery        = new Delivery();
        delivery.deliveryType    =
            (String) request.get("deliveryType");
        delivery.conductedBy     = userName;
        delivery.placentaComplete =
            request.get("placentaComplete") == null ||
            Boolean.parseBoolean(
                request.get("placentaComplete").toString());
        delivery.bloodLossMl =
            request.get("bloodLossMl") != null
            ? Integer.parseInt(
                request.get("bloodLossMl").toString()) : 0;
        delivery.episiotomy =
            request.get("episiotomy") != null &&
            Boolean.parseBoolean(
                request.get("episiotomy").toString());
        delivery.perinealTear =
            request.get("perinealTear") != null
            ? (String) request.get("perinealTear") : "NONE";
        delivery.oxytocinGiven =
            request.get("oxytocinGiven") == null ||
            Boolean.parseBoolean(
                request.get("oxytocinGiven").toString());
        delivery.maternalCondition =
            request.get("maternalCondition") != null
            ? (String) request.get("maternalCondition")
            : "GOOD";
        delivery.maternalComplications =
            (String) request.get("maternalComplications");
        delivery.notes = (String) request.get("notes");

        List<Map<String, Object>> babies =
            (List<Map<String, Object>>)
            request.get("babies");

        return Response.status(201)
            .entity(maternityService.recordDelivery(
                maternityAdmissionId, delivery,
                babies, userId, userName))
            .build();
    }

    @GET
    @Path("/deliveries/today")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<Delivery> getTodaysDeliveries() {
        return maternityService.getTodaysDeliveries();
    }

    @GET
    @Path("/deliveries/month/{year}/{month}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<Delivery> getDeliveriesByMonth(
            @PathParam("year") int year,
            @PathParam("month") int month) {
        return maternityService
            .getDeliveriesByMonth(year, month);
    }

    @GET
    @Path("/babies/delivery/{deliveryId}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<BabyRecord> getBabiesByDelivery(
            @PathParam("deliveryId") Long deliveryId) {
        return maternityService
            .getBabiesByDelivery(deliveryId);
    }

    @GET
    @Path("/babies/low-birth-weight")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<BabyRecord> getLowBirthWeightBabies() {
        return maternityService.getLowBirthWeightBabies();
    }

    @POST
    @Path("/postnatal/{deliveryId}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public Response recordPostnatalVisit(
            @PathParam("deliveryId") Long deliveryId,
            PostnatalVisit request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        request.seenBy  = userName;
        return Response.status(201)
            .entity(maternityService.recordPostnatalVisit(
                deliveryId, request, userId, userName))
            .build();
    }

    @GET
    @Path("/postnatal/{deliveryId}")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<PostnatalVisit> getPostnatalVisits(
            @PathParam("deliveryId") Long deliveryId) {
        return maternityService
            .getPostnatalVisits(deliveryId);
    }

    @GET
    @Path("/postnatal/risk-flags")
    @RolesAllowed({"NURSE","DOCTOR","CLINICAL_OFFICER",
                   "ADMIN"})
    public List<PostnatalVisit> getPostnatalWithRiskFlags() {
        return maternityService
            .getPostnatalWithRiskFlags();
    }
}