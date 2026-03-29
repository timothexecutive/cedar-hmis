package ke.cedar.hmis.theatre;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

@Path("/api/theatre")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TheatreResource {

    @Inject TheatreService theatreService;
    @Inject JsonWebToken   jwt;

    @GET
    @Path("/rooms")
    @RolesAllowed({"ADMIN","DOCTOR","NURSE",
                   "CLINICAL_OFFICER"})
    public List<TheatreRoom> getAllRooms() {
        return theatreService.getAllRooms();
    }

    @GET
    @Path("/rooms/available")
    @RolesAllowed({"ADMIN","DOCTOR","NURSE",
                   "CLINICAL_OFFICER"})
    public List<TheatreRoom> getAvailableRooms() {
        return theatreService.getAvailableRooms();
    }

    @POST
    @Path("/bookings")
    @RolesAllowed({"ADMIN","DOCTOR","CLINICAL_OFFICER"})
    public Response bookSurgery(
            Map<String, Object> request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();

        Long patientId = Long.parseLong(
            request.get("patientId").toString());
        Long theatreRoomId = Long.parseLong(
            request.get("theatreRoomId").toString());
        Long ipdAdmissionId =
            request.get("ipdAdmissionId") != null
            ? Long.parseLong(
                request.get("ipdAdmissionId").toString())
            : null;
        Long maternityId =
            request.get("maternityAdmissionId") != null
            ? Long.parseLong(
                request.get("maternityAdmissionId")
                .toString()) : null;

        SurgeryBooking booking  = new SurgeryBooking();
        booking.surgeryType     =
            (String) request.get("surgeryType");
        booking.surgeryCategory =
            request.get("surgeryCategory") != null
            ? (String) request.get("surgeryCategory")
            : "ELECTIVE";
        booking.diagnosis =
            (String) request.get("diagnosis");
        booking.plannedDate = java.time.LocalDate.parse(
            request.get("plannedDate").toString());
        booking.plannedStartTime =
            request.get("plannedStartTime") != null
            ? java.time.LocalTime.parse(
                request.get("plannedStartTime").toString())
            : null;
        booking.plannedDuration =
            request.get("plannedDuration") != null
            ? Integer.parseInt(
                request.get("plannedDuration").toString())
            : null;
        booking.leadSurgeon =
            (String) request.get("leadSurgeon");
        booking.assistantSurgeon =
            (String) request.get("assistantSurgeon");
        booking.anaesthetist =
            (String) request.get("anaesthetist");
        booking.scrubNurse =
            (String) request.get("scrubNurse");
        booking.circulatingNurse =
            (String) request.get("circulatingNurse");
        booking.anaesthesiaType =
            (String) request.get("anaesthesiaType");
        booking.bloodOrdered =
            request.get("bloodOrdered") != null &&
            Boolean.parseBoolean(
                request.get("bloodOrdered").toString());
        booking.bloodUnits =
            request.get("bloodUnits") != null
            ? Integer.parseInt(
                request.get("bloodUnits").toString()) : 0;
        booking.consentSigned =
            request.get("consentSigned") != null &&
            Boolean.parseBoolean(
                request.get("consentSigned").toString());
        booking.bookedBy = userName;
        booking.notes    =
            (String) request.get("notes");

        return Response.status(201)
            .entity(theatreService.bookSurgery(
                patientId, theatreRoomId,
                ipdAdmissionId, maternityId,
                booking, userId, userName))
            .build();
    }

    @GET
    @Path("/bookings/today")
    @RolesAllowed({"ADMIN","DOCTOR","NURSE",
                   "CLINICAL_OFFICER"})
    public List<SurgeryBooking> getTodaysSurgeries() {
        return theatreService.getTodaysSurgeries();
    }

    @GET
    @Path("/bookings/status/{status}")
    @RolesAllowed({"ADMIN","DOCTOR","NURSE",
                   "CLINICAL_OFFICER"})
    public List<SurgeryBooking> getByStatus(
            @PathParam("status") String status) {
        return theatreService.getByStatus(status);
    }

    @GET
    @Path("/bookings/patient/{patientId}")
    @RolesAllowed({"ADMIN","DOCTOR","NURSE",
                   "CLINICAL_OFFICER"})
    public List<SurgeryBooking> getPatientSurgeries(
            @PathParam("patientId") Long patientId) {
        return theatreService
            .getPatientSurgeries(patientId);
    }

    @GET
    @Path("/bookings/month/{year}/{month}")
    @RolesAllowed({"ADMIN","DOCTOR","NURSE",
                   "CLINICAL_OFFICER"})
    public List<SurgeryBooking> getSurgeriesByMonth(
            @PathParam("year") int year,
            @PathParam("month") int month) {
        return theatreService
            .getSurgeriesByMonth(year, month);
    }

    @PUT
    @Path("/bookings/{bookingId}/cancel")
    @RolesAllowed({"ADMIN","DOCTOR","CLINICAL_OFFICER"})
    public Response cancelSurgery(
            @PathParam("bookingId") Long bookingId,
            Map<String, Object> request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        return Response.ok(
            theatreService.cancelSurgery(
                bookingId,
                (String) request.get("reason"),
                userName,
                userId, userName))
            .build();
    }

    @POST
    @Path("/bookings/{bookingId}/preop")
    @RolesAllowed({"ADMIN","NURSE","DOCTOR",
                   "CLINICAL_OFFICER"})
    public Response submitPreOp(
            @PathParam("bookingId") Long bookingId,
            PreOpChecklist request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        request.completedBy = userName;
        return Response.status(201)
            .entity(theatreService.submitPreOp(
                bookingId, request, userId, userName))
            .build();
    }

    @GET
    @Path("/bookings/{bookingId}/preop")
    @RolesAllowed({"ADMIN","NURSE","DOCTOR",
                   "CLINICAL_OFFICER"})
    public PreOpChecklist getPreOp(
            @PathParam("bookingId") Long bookingId) {
        return theatreService.getPreOp(bookingId);
    }

    @POST
    @Path("/bookings/{bookingId}/intraop")
    @RolesAllowed({"ADMIN","NURSE","DOCTOR",
                   "CLINICAL_OFFICER"})
    public Response recordIntraOp(
            @PathParam("bookingId") Long bookingId,
            IntraOpRecord request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        request.recordedBy = userName;
        return Response.status(201)
            .entity(theatreService.recordIntraOp(
                bookingId, request, userId, userName))
            .build();
    }

    @GET
    @Path("/bookings/{bookingId}/intraop")
    @RolesAllowed({"ADMIN","NURSE","DOCTOR",
                   "CLINICAL_OFFICER"})
    public IntraOpRecord getIntraOp(
            @PathParam("bookingId") Long bookingId) {
        return theatreService.getIntraOp(bookingId);
    }

    @POST
    @Path("/bookings/{bookingId}/postop")
    @RolesAllowed({"ADMIN","NURSE","DOCTOR",
                   "CLINICAL_OFFICER"})
    public Response recordPostOp(
            @PathParam("bookingId") Long bookingId,
            PostOpRecord request) {
        Long   userId   = jwt.getClaim("userId");
        String userName = jwt.getName();
        request.recordedBy = userName;
        return Response.status(201)
            .entity(theatreService.recordPostOp(
                bookingId, request, userId, userName))
            .build();
    }

    @GET
    @Path("/bookings/{bookingId}/postop")
    @RolesAllowed({"ADMIN","NURSE","DOCTOR",
                   "CLINICAL_OFFICER"})
    public List<PostOpRecord> getPostOp(
            @PathParam("bookingId") Long bookingId) {
        return theatreService.getPostOp(bookingId);
    }
}