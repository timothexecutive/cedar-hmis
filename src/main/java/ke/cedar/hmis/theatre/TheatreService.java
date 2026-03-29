package ke.cedar.hmis.theatre;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.ipd.Admission;
import ke.cedar.hmis.maternity.MaternityAdmission;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@ApplicationScoped
public class TheatreService {

    @Transactional
    public SurgeryBooking bookSurgery(Long patientId,
            Long theatreRoomId, Long ipdAdmissionId,
            Long maternityAdmissionId,
            SurgeryBooking request,
            Long userId, String userName) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        TheatreRoom room =
            TheatreRoom.findById(theatreRoomId);
        if (room == null || !room.isActive) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Theatre room " +
                            "not found\"}")
                    .build());
        }

        if (request.plannedStartTime != null &&
                request.plannedDuration != null) {
            boolean conflict = SurgeryBooking.hasConflict(
                theatreRoomId,
                request.plannedDate,
                request.plannedStartTime,
                request.plannedDuration);
            if (conflict) {
                throw new WebApplicationException(
                    Response.status(409)
                        .entity("{\"error\":\"Theatre room " +
                                "already booked at this " +
                                "time\"}")
                        .build());
            }
        }

        String bookingNo = String.format("SRG-%d-%05d",
            Year.now().getValue(),
            SurgeryBooking.count() + 1);

        request.bookingNo   = bookingNo;
        request.patient     = patient;
        request.theatreRoom = room;
        request.bookedBy    = userName;

        if (ipdAdmissionId != null) {
            Admission adm =
                Admission.findById(ipdAdmissionId);
            if (adm != null)
                request.ipdAdmission = adm;
        }

        if (maternityAdmissionId != null) {
            MaternityAdmission mat =
                MaternityAdmission
                    .findById(maternityAdmissionId);
            if (mat != null)
                request.maternityAdmission = mat;
        }

        request.persist();
        room.status = "BOOKED";
        room.persist();

        AuditTrail.log(
            userId, userName, "DOCTOR",
            "SURGERY_BOOKED", "THEATRE",
            "SurgeryBooking",
            String.valueOf(request.id),
            "Surgery booked: " + bookingNo +
            " | Patient: " + patient.fullName +
            " | Type: " + request.surgeryType +
            " | Category: " + request.surgeryCategory +
            " | Room: " + room.name +
            " | Date: " + request.plannedDate +
            " | Surgeon: " + request.leadSurgeon +
            " | By: " + userName);

        return request;
    }

    @Transactional
    public SurgeryBooking cancelSurgery(Long bookingId,
            String reason, String cancelledBy,
            Long userId, String userName) {

        SurgeryBooking booking =
            SurgeryBooking.findById(bookingId);
        if (booking == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Booking " +
                            "not found\"}")
                    .build());
        }
        if (booking.status.equals("COMPLETED")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Cannot cancel " +
                            "completed surgery\"}")
                    .build());
        }
        if (reason == null || reason.isBlank()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Cancellation " +
                            "reason is mandatory\"}")
                    .build());
        }

        booking.status             = "CANCELLED";
        booking.cancellationReason = reason;
        booking.persist();

        booking.theatreRoom.status = "AVAILABLE";
        booking.theatreRoom.persist();

        AuditTrail.logChange(
            userId, userName, "DOCTOR",
            "SURGERY_CANCELLED", "THEATRE",
            "SurgeryBooking",
            String.valueOf(bookingId),
            "SCHEDULED", "CANCELLED",
            "Surgery cancelled: " +
            booking.bookingNo +
            " | Patient: " +
            booking.patient.fullName +
            " | Reason: " + reason +
            " | By: " + userName);

        return booking;
    }

    @Transactional
    public PreOpChecklist submitPreOp(Long bookingId,
            PreOpChecklist request,
            Long userId, String userName) {

        SurgeryBooking booking =
            SurgeryBooking.findById(bookingId);
        if (booking == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Booking " +
                            "not found\"}")
                    .build());
        }
        if (booking.status.equals("CANCELLED")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Surgery is " +
                            "cancelled\"}")
                    .build());
        }

        request.booking      = booking;
        request.patient      = booking.patient;
        request.completedBy  = userName;

        if (!request.isSafeToOperate()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Pre-op checklist " +
                            "incomplete. Patient is not safe " +
                            "to proceed to theatre.\"}")
                    .build());
        }

        request.persist();
        booking.status = "IN_PROGRESS";
        booking.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "PREOP_COMPLETED", "THEATRE",
            "SurgeryBooking",
            String.valueOf(bookingId),
            "Pre-op completed: " +
            booking.bookingNo +
            " | Patient: " +
            booking.patient.fullName +
            " | BP: " + request.bpSystolic +
            "/" + request.bpDiastolic +
            " | SpO2: " + request.spo2 +
            " | Fasting: " +
            request.fastingHours + "hrs" +
            " | Safe: YES" +
            " | By: " + userName);

        return request;
    }

    @Transactional
    public IntraOpRecord recordIntraOp(Long bookingId,
            IntraOpRecord request,
            Long userId, String userName) {

        SurgeryBooking booking =
            SurgeryBooking.findById(bookingId);
        if (booking == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Booking " +
                            "not found\"}")
                    .build());
        }
        if (!booking.status.equals("IN_PROGRESS")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Pre-op must be " +
                            "completed first\"}")
                    .build());
        }
        if (!request.isSignOutComplete()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"WHO Sign Out " +
                            "incomplete. Instruments and " +
                            "swabs must be counted.\"}")
                    .build());
        }

        request.booking    = booking;
        request.patient    = booking.patient;
        request.recordedBy = userName;
        request.persist();

        booking.status = "COMPLETED";
        booking.theatreRoom.status = "AVAILABLE";
        booking.theatreRoom.persist();
        booking.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "SURGERY_COMPLETED", "THEATRE",
            "SurgeryBooking",
            String.valueOf(bookingId),
            "Surgery completed: " +
            booking.bookingNo +
            " | Patient: " +
            booking.patient.fullName +
            " | Procedure: " +
            request.procedurePerformed +
            " | Duration: " +
            request.durationMinutes + "mins" +
            " | Blood loss: " +
            request.bloodLossMl + "ml" +
            " | Outcome: " + request.surgeryOutcome +
            " | By: " + userName);

        return request;
    }

    @Transactional
    public PostOpRecord recordPostOp(Long bookingId,
            PostOpRecord request,
            Long userId, String userName) {

        SurgeryBooking booking =
            SurgeryBooking.findById(bookingId);
        if (booking == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Booking " +
                            "not found\"}")
                    .build());
        }

        request.booking    = booking;
        request.patient    = booking.patient;
        request.recordedBy = userName;
        request.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "POSTOP_RECORDED", "THEATRE",
            "SurgeryBooking",
            String.valueOf(bookingId),
            "Post-op: " + booking.bookingNo +
            " | Patient: " +
            booking.patient.fullName +
            " | BP: " + request.bpSystolic +
            "/" + request.bpDiastolic +
            " | SpO2: " + request.spo2 +
            " | Pain: " + request.painScore +
            " | Aldrete: " + request.aldreteScore +
            " | Safe to discharge: " +
            (request.isSafeToDischarge() ?
                "YES" : "NO") +
            " | By: " + userName);

        return request;
    }

    public List<TheatreRoom> getAllRooms() {
        return TheatreRoom.findAllActive(); }
    public List<TheatreRoom> getAvailableRooms() {
        return TheatreRoom.findAvailable(); }
    public List<SurgeryBooking> getTodaysSurgeries() {
        return SurgeryBooking.findByDate(
            LocalDate.now()); }
    public List<SurgeryBooking> getByStatus(String s) {
        return SurgeryBooking.findByStatus(s); }
    public List<SurgeryBooking> getPatientSurgeries(
            Long p) {
        return SurgeryBooking.findByPatient(p); }
    public List<SurgeryBooking> getSurgeriesByMonth(
            int y, int m) {
        return SurgeryBooking.findByMonth(y, m); }
    public PreOpChecklist getPreOp(Long id) {
        return PreOpChecklist.findByBooking(id); }
    public IntraOpRecord getIntraOp(Long id) {
        return IntraOpRecord.findByBooking(id); }
    public List<PostOpRecord> getPostOp(Long id) {
        return PostOpRecord.findByBooking(id); }
}