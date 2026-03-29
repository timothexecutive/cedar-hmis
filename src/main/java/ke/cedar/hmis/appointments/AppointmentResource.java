package ke.cedar.hmis.appointments;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Path("/api/appointments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppointmentResource {

    @Inject AppointmentService appointmentService;
    @Inject JsonWebToken        jwt;

    private Long getUserId() {
        Object claim = jwt.getClaim("userId");
        return claim != null ?
            Long.parseLong(claim.toString()) : null;
    }

    // ── Doctor Schedules ──────────────────────────────
    @GET
    @Path("/schedules")
    @RolesAllowed({"ADMIN","RECEPTIONIST","DOCTOR",
                   "NURSE","CLINICAL_OFFICER"})
    public List<DoctorSchedule> getAllSchedules() {
        return appointmentService.getAllSchedules();
    }

    @GET
    @Path("/schedules/{scheduleId}/slots")
    @RolesAllowed({"ADMIN","RECEPTIONIST","DOCTOR",
                   "NURSE","CLINICAL_OFFICER"})
    public List<Map<String, Object>> getAvailableSlots(
            @PathParam("scheduleId") Long scheduleId,
            @QueryParam("date") String dateStr) {

        LocalDate date = dateStr != null
            ? LocalDate.parse(dateStr)
            : LocalDate.now();

        return appointmentService
            .getAvailableSlots(scheduleId, date);
    }

    // ── Book appointment ──────────────────────────────
    @POST
    @Path("/book")
    @Transactional
    @RolesAllowed({"ADMIN","RECEPTIONIST","NURSE",
                   "DOCTOR","CLINICAL_OFFICER"})
    public Response bookAppointment(
            Map<String, Object> request) {

        Long   userId   = getUserId();
        String userName = jwt.getName();

        if (request.get("scheduleId") == null ||
                request.get("appointmentDate") == null ||
                request.get("appointmentTime") == null ||
                request.get("patientName") == null ||
                request.get("patientPhone") == null) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"scheduleId, " +
                            "appointmentDate, " +
                            "appointmentTime, " +
                            "patientName and patientPhone " +
                            "are required\"}")
                    .build());
        }

        return Response.status(201)
            .entity(appointmentService.bookAppointment(
                Long.parseLong(
                    request.get("scheduleId").toString()),
                LocalDate.parse(
                    request.get("appointmentDate")
                    .toString()),
                LocalTime.parse(
                    request.get("appointmentTime")
                    .toString()),
                (String) request.get("patientName"),
                (String) request.get("patientPhone"),
                (String) request.get("patientNationalId"),
                (String) request.get("reason"),
                (String) request.get("paymentType"),
                (String) request.get("insuranceProvider"),
                (String) request.get("insuranceMemberNo"),
                (String) request.get("bookingSource"),
                userId, userName))
            .build();
    }

    // ── Today's appointments ──────────────────────────
    @GET
    @Path("/today")
    @RolesAllowed({"ADMIN","RECEPTIONIST","DOCTOR",
                   "NURSE","CLINICAL_OFFICER"})
    public List<Appointment> getTodaysAppointments() {
        return appointmentService
            .getTodaysAppointments();
    }

    // ── Appointments by date ──────────────────────────
    @GET
    @Path("/date/{date}")
    @RolesAllowed({"ADMIN","RECEPTIONIST","DOCTOR",
                   "NURSE","CLINICAL_OFFICER"})
    public List<Appointment> getByDate(
            @PathParam("date") String dateStr) {
        return appointmentService
            .getAppointmentsByDate(
                LocalDate.parse(dateStr));
    }

    // ── Appointments by patient ───────────────────────
    @GET
    @Path("/patient/{patientId}")
    @RolesAllowed({"ADMIN","RECEPTIONIST","DOCTOR",
                   "NURSE","CLINICAL_OFFICER"})
    public List<Appointment> getByPatient(
            @PathParam("patientId") Long patientId) {
        return appointmentService.getByPatient(patientId);
    }

    // ── Search by phone ───────────────────────────────
    @GET
    @Path("/search")
    @RolesAllowed({"ADMIN","RECEPTIONIST","NURSE"})
    public List<Appointment> searchByPhone(
            @QueryParam("phone") String phone) {
        if (phone == null || phone.isBlank()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Phone number " +
                            "is required\"}")
                    .build());
        }
        return appointmentService.getByPhone(phone);
    }

    // ── Today's arrived patients ──────────────────────
    @GET
    @Path("/arrived")
    @RolesAllowed({"ADMIN","RECEPTIONIST","NURSE",
                   "DOCTOR","CLINICAL_OFFICER"})
    public List<Appointment> getTodayArrived() {
        return appointmentService.getTodayArrived();
    }

    // ── Mark patient arrived ──────────────────────────
    @PUT
    @Path("/{appointmentId}/arrive")
    @Transactional
    @RolesAllowed({"ADMIN","RECEPTIONIST","NURSE"})
    public Response markArrived(
            @PathParam("appointmentId")
            Long appointmentId) {

        Long   userId   = getUserId();
        String userName = jwt.getName();

        return Response.ok(
            appointmentService.markArrived(
                appointmentId, userId, userName))
            .build();
    }

    // ── Cancel appointment ────────────────────────────
    @PUT
    @Path("/{appointmentId}/cancel")
    @Transactional
    @RolesAllowed({"ADMIN","RECEPTIONIST","NURSE",
                   "DOCTOR","CLINICAL_OFFICER"})
    public Response cancelAppointment(
            @PathParam("appointmentId")
            Long appointmentId,
            Map<String, Object> request) {

        Long   userId   = getUserId();
        String userName = jwt.getName();

        return Response.ok(
            appointmentService.cancelAppointment(
                appointmentId,
                (String) request.get("reason"),
                userId, userName))
            .build();
    }

    // ── Mark no-shows for a date ──────────────────────
    @PUT
    @Path("/no-shows")
    @Transactional
    @RolesAllowed({"ADMIN","RECEPTIONIST"})
    public Response markNoShows(
            Map<String, Object> request) {

        Long   userId   = getUserId();
        String userName = jwt.getName();

        LocalDate date = request.get("date") != null
            ? LocalDate.parse(
                request.get("date").toString())
            : LocalDate.now();

        int count = appointmentService
            .markNoShows(date, userId, userName);

        return Response.ok(Map.of(
            "message", count + " no-shows marked",
            "date", date.toString(),
            "count", count)).build();
    }
}