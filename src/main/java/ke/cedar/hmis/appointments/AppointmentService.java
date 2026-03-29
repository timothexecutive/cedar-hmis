package ke.cedar.hmis.appointments;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
import ke.cedar.hmis.billing.MpesaService;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class AppointmentService {

    @Inject
    SmsService smsService;

    @Inject
    MpesaService mpesaService;

    // ── Get available slots for a doctor on a date ────
    public List<Map<String, Object>> getAvailableSlots(
            Long scheduleId, LocalDate date) {

        DoctorSchedule schedule =
            DoctorSchedule.findById(scheduleId);
        if (schedule == null || !schedule.isActive) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Doctor schedule " +
                            "not found\"}")
                    .build());
        }

        // Check doctor works on this day
        String dayCode = date.getDayOfWeek()
            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            .toUpperCase().substring(0, 3);

        if (!schedule.worksOnDay(dayCode)) {
            return new ArrayList<>();
        }

        // Don't allow booking in the past
        if (date.isBefore(LocalDate.now())) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Cannot book " +
                            "appointments in the past\"}")
                    .build());
        }

        // Generate all possible slots
        List<LocalTime> allSlots = schedule.generateSlots();

        // Get already booked slots for this date
        List<Appointment> booked =
            Appointment.findByDoctor(scheduleId, date);
        List<LocalTime> bookedTimes = new ArrayList<>();
        for (Appointment a : booked) {
            if (!"CANCELLED".equals(a.status) &&
                    !"NO_SHOW".equals(a.status)) {
                bookedTimes.add(a.appointmentTime);
            }
        }

        // Build slot map with availability
        List<Map<String, Object>> slots = new ArrayList<>();
        for (LocalTime slot : allSlots) {
            // Skip past slots if today
            boolean isPast = date.equals(LocalDate.now())
                && slot.isBefore(LocalTime.now());

            Map<String, Object> slotInfo = new HashMap<>();
            slotInfo.put("time", slot.toString());
            slotInfo.put("available",
                !bookedTimes.contains(slot) && !isPast);
            slotInfo.put("status",
                isPast ? "PASSED"
                : bookedTimes.contains(slot) ?
                    "BOOKED" : "AVAILABLE");
            slotInfo.put("doctor", schedule.doctorName);
            slotInfo.put("department", schedule.department);
            slotInfo.put("fee", schedule.consultationFee);
            slots.add(slotInfo);
        }

        return slots;
    }

    // ── Book an appointment ───────────────────────────
    // ONLINE + CASH  → M-Pesa STK push, SMS held
    // RECEPTION      → confirmed immediately, SMS fires
    // INSURANCE/SHA  → confirmed immediately, SMS fires
    @Transactional
    public Appointment bookAppointment(
            Long scheduleId,
            LocalDate appointmentDate,
            LocalTime appointmentTime,
            String patientName,
            String patientPhone,
            String patientNationalId,
            String reason,
            String paymentType,
            String insuranceProvider,
            String insuranceMemberNo,
            String bookingSource,
            Long userId, String userName) {

        DoctorSchedule schedule =
            DoctorSchedule.findById(scheduleId);
        if (schedule == null || !schedule.isActive) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Doctor schedule " +
                            "not found\"}")
                    .build());
        }

        // Validate date is not in the past
        if (appointmentDate.isBefore(LocalDate.now())) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Cannot book " +
                            "appointment in the past\"}")
                    .build());
        }

        // Check doctor works that day
        String dayCode = appointmentDate.getDayOfWeek()
            .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            .toUpperCase().substring(0, 3);
        if (!schedule.worksOnDay(dayCode)) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Doctor does not " +
                            "work on " + dayCode + "\"}")
                    .build());
        }

        // Bulletproof slot check — database enforces UNIQUE
        if (Appointment.slotTaken(scheduleId,
                appointmentDate, appointmentTime)) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"This slot is " +
                            "already taken. Please choose " +
                            "another time.\"}")
                    .build());
        }

        // Validate payment type
        if (paymentType == null || paymentType.isBlank()) {
            paymentType = "CASH";
        }
        paymentType = paymentType.toUpperCase();

        // Insurance/SHA needs provider + member number
        if (("INSURANCE".equals(paymentType) ||
                "SHA".equals(paymentType)) &&
                (insuranceProvider == null ||
                insuranceProvider.isBlank())) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Insurance " +
                            "provider is required for " +
                            "insurance bookings\"}")
                    .build());
        }

        // Try to find existing patient record
        Patient patient = null;
        if (patientNationalId != null &&
                !patientNationalId.isBlank()) {
            patient = Patient.findByNationalId(
                patientNationalId);
        }
        if (patient == null && patientPhone != null) {
            List<Patient> byPhone = Patient.find(
                "phone = ?1", patientPhone).list();
            if (byPhone.size() == 1) {
                patient = byPhone.get(0);
            }
        }

        // Generate appointment number
        String apptNo = "APT-" +
            LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd")) +
            "-" + String.format("%04d",
                Appointment.count() + 1);

        Appointment appointment = new Appointment();
        appointment.appointmentNo     = apptNo;
        appointment.schedule          = schedule;
        appointment.patient           = patient;
        appointment.patientName       = patientName;
        appointment.patientPhone      = patientPhone;
        appointment.patientNationalId = patientNationalId;
        appointment.doctorName        = schedule.doctorName;
        appointment.department        = schedule.department;
        appointment.appointmentDate   = appointmentDate;
        appointment.appointmentTime   = appointmentTime;
        appointment.reason            = reason;
        appointment.paymentType       = paymentType;
        appointment.consultationFee   = schedule.consultationFee;
        appointment.insuranceProvider = insuranceProvider;
        appointment.insuranceMemberNo = insuranceMemberNo;
        appointment.bookingSource     = bookingSource != null
            ? bookingSource : "RECEPTION";
        appointment.bookedBy          = userName;

        // ── Determine status and trigger actions ──────
        boolean isOnlineCash = "ONLINE".equals(
            appointment.bookingSource) &&
            "CASH".equals(paymentType);

        boolean isInsurance =
            "INSURANCE".equals(paymentType) ||
            "SHA".equals(paymentType);

        if (isOnlineCash) {
            // Online cash — wait for M-Pesa confirmation
            // before confirming appointment
            appointment.status = "PENDING_PAYMENT";
        } else {
            // Reception cash or insurance — confirm now
            appointment.status = "CONFIRMED";
        }

        appointment.persist();

        AuditTrail.log(
            userId, userName, "RECEPTIONIST",
            "APPOINTMENT_BOOKED", "APPOINTMENTS",
            "Appointment",
            String.valueOf(appointment.id),
            "Appointment booked: " + apptNo +
            " | Patient: " + patientName +
            " | Doctor: " + schedule.doctorName +
            " | Date: " + appointmentDate +
            " | Time: " + appointmentTime +
            " | Payment: " + paymentType +
            " | Source: " + appointment.bookingSource +
            " | Status: " + appointment.status +
            " | By: " + userName);

        // ── Fire M-Pesa STK push for online cash ──────
        if (isOnlineCash) {
            try {
                String checkoutId =
                    mpesaService.initiateSTKPush(
                        patientPhone,
                        schedule.consultationFee
                            .intValue(),
                        "APT-" + appointment.id,
                        "Cedar Hospital Appointment");

                // Store checkout ID for callback matching
                appointment.mpesaCheckoutId = checkoutId;
                appointment.persist();

                System.out.println(
                    "[APPOINTMENT] M-Pesa STK push " +
                    "sent for " + apptNo +
                    " | CheckoutID: " + checkoutId);

                // SMS will fire from confirmOnlinePayment()
                // when M-Pesa callback confirms payment

            } catch (Exception e) {
                System.err.println(
                    "[APPOINTMENT] M-Pesa STK push " +
                    "failed for " + apptNo +
                    ": " + e.getMessage());
                // Don't crash — appointment saved
                // Receptionist can follow up manually
            }

        } else {
            // Reception or insurance — fire SMS now
            String dateStr = appointmentDate.format(
                DateTimeFormatter.ofPattern("dd MMM yyyy"));
            String timeStr = appointmentTime.format(
                DateTimeFormatter.ofPattern("hh:mm a"));

            try {
                smsService.sendAppointmentConfirmation(
                    patientPhone, patientName,
                    schedule.doctorName,
                    dateStr, timeStr, paymentType);
            } catch (Exception e) {
                System.err.println(
                    "[SMS FAILED] Appointment booked " +
                    "but SMS failed: " + e.getMessage());
            }
        }

        return appointment;
    }

    // ── Confirm online payment + fire SMS ─────────────
    // Called from M-Pesa callback when payment confirmed
    @Transactional
    public void confirmOnlinePayment(
            String mpesaCheckoutId,
            String mpesaReceipt) {

        // Find appointment by checkout ID
        Appointment appointment = Appointment.find(
            "mpesaCheckoutId = ?1",
            mpesaCheckoutId).firstResult();

        if (appointment == null) {
            System.out.println(
                "[APPOINTMENT] No appointment found " +
                "for checkout ID: " + mpesaCheckoutId);
            return;
        }

        // Update appointment payment status
        appointment.status          = "CONFIRMED";
        appointment.paymentVerified = true;
        appointment.mpesaReceipt    = mpesaReceipt;
        appointment.persist();

        System.out.println(
            "[APPOINTMENT] Payment confirmed: " +
            appointment.appointmentNo +
            " | Receipt: " + mpesaReceipt);

        // NOW fire the SMS confirmation
        String dateStr = appointment.appointmentDate
            .format(DateTimeFormatter
                .ofPattern("dd MMM yyyy"));
        String timeStr = appointment.appointmentTime
            .format(DateTimeFormatter
                .ofPattern("hh:mm a"));

        try {
            smsService.sendAppointmentConfirmation(
                appointment.patientPhone,
                appointment.patientName,
                appointment.doctorName,
                dateStr, timeStr,
                appointment.paymentType);
        } catch (Exception e) {
            System.err.println(
                "[SMS FAILED] Post-payment SMS: " +
                e.getMessage());
        }

        AuditTrail.log(
            null, "Safaricom", "SYSTEM",
            "APPOINTMENT_PAYMENT_CONFIRMED",
            "APPOINTMENTS",
            "Appointment",
            String.valueOf(appointment.id),
            "Online payment confirmed: " +
            appointment.appointmentNo +
            " | Patient: " + appointment.patientName +
            " | Receipt: " + mpesaReceipt +
            " | Amount: KES " +
            appointment.consultationFee);
    }

    // ── Mark patient as arrived ───────────────────────
    @Transactional
    public Appointment markArrived(Long appointmentId,
            Long userId, String userName) {

        Appointment appointment =
            Appointment.findById(appointmentId);
        if (appointment == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Appointment " +
                            "not found\"}")
                    .build());
        }

        if ("CANCELLED".equals(appointment.status)) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Appointment " +
                            "is cancelled\"}")
                    .build());
        }

        if ("PENDING_PAYMENT".equals(
                appointment.status)) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Payment not " +
                            "confirmed for this " +
                            "appointment. Please verify " +
                            "M-Pesa payment first.\"}")
                    .build());
        }

        if ("ARRIVED".equals(appointment.status) ||
                "COMPLETED".equals(appointment.status)) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Patient has " +
                            "already arrived\"}")
                    .build());
        }

        // Find or register patient
        Patient patient = appointment.patient;

        if (patient == null) {
            // National ID first — most reliable
            if (appointment.patientNationalId != null &&
                    !appointment.patientNationalId
                    .isBlank()) {
                patient = Patient.findByNationalId(
                    appointment.patientNationalId);
            }

            // Phone fallback — only if one match
            if (patient == null) {
                List<Patient> byPhone = Patient.find(
                    "phone = ?1",
                    appointment.patientPhone).list();
                if (byPhone.size() == 1) {
                    patient = byPhone.get(0);
                }
            }

            // Auto-register if still not found
            if (patient == null) {
                long count = Patient.countActive() + 1;
                patient = new Patient();
                patient.fullName   = appointment.patientName;
                patient.phone      = appointment.patientPhone;
                patient.nationalId =
                    appointment.patientNationalId;
                patient.gender     = "Unknown";
                patient.patientNo  = String.format(
                    "CDR-%d-%05d",
                    Year.now().getValue(), count);
                patient.isSHAMember =
                    "SHA".equals(appointment.paymentType);
                patient.registeredBy = userName;
                patient.persist();

                AuditTrail.log(
                    userId, userName, "RECEPTIONIST",
                    "PATIENT_REGISTERED", "RECEPTION",
                    "Patient", String.valueOf(patient.id),
                    "Auto-registered from appointment: " +
                    patient.fullName +
                    " | PatientNo: " + patient.patientNo +
                    " | By: " + userName);
            }

            appointment.patient = patient;
        }

        // Create Visit — joins OPD triage queue
        long todayCount = Visit.countTodaysQueue() + 1;
        String visitNo = String.format("V%s-%03d",
            LocalDate.now().toString().replace("-", ""),
            todayCount);

        Visit visit = new Visit();
        visit.patient        = patient;
        visit.visitNo        = visitNo;
        visit.queueNumber    = (int) todayCount;
        visit.chiefComplaint = appointment.reason != null
            ? appointment.reason
            : "Appointment with " +
              appointment.doctorName;
        visit.assignedDoctor = appointment.doctorName;
        visit.status         = "WAITING";
        visit.visitType      = "OPD";
        visit.persist();

        appointment.status          = "ARRIVED";
        appointment.arrivedAt       = LocalDateTime.now();
        appointment.markedArrivedBy = userName;
        appointment.visit           = visit;
        appointment.persist();

        AuditTrail.log(
            userId, userName, "RECEPTIONIST",
            "APPOINTMENT_ARRIVED", "APPOINTMENTS",
            "Appointment",
            String.valueOf(appointmentId),
            "Patient arrived: " +
            appointment.patientName +
            " | Appt: " + appointment.appointmentNo +
            " | Visit: " + visitNo +
            " | Queue#: " + visit.queueNumber +
            " | By: " + userName);

        // Arrival SMS
        try {
            smsService.sendArrivalConfirmation(
                appointment.patientPhone,
                appointment.patientName,
                appointment.doctorName);
        } catch (Exception e) {
            System.err.println(
                "[SMS FAILED] Arrival SMS: " +
                e.getMessage());
        }

        return appointment;
    }

    // ── Cancel appointment ────────────────────────────
    @Transactional
    public Appointment cancelAppointment(
            Long appointmentId, String reason,
            Long userId, String userName) {

        Appointment appointment =
            Appointment.findById(appointmentId);
        if (appointment == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Appointment " +
                            "not found\"}")
                    .build());
        }

        if ("CANCELLED".equals(appointment.status)) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Appointment " +
                            "already cancelled\"}")
                    .build());
        }

        if ("COMPLETED".equals(appointment.status)) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Cannot cancel " +
                            "completed appointment\"}")
                    .build());
        }

        if (reason == null || reason.isBlank()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Cancellation " +
                            "reason is mandatory\"}")
                    .build());
        }

        boolean eligibleForRefund =
            appointment.canBeCancelled() &&
            Boolean.TRUE.equals(
                appointment.paymentVerified) &&
            "CASH".equals(appointment.paymentType);

        String oldStatus = appointment.status;
        appointment.status             = "CANCELLED";
        appointment.cancellationReason = reason;
        appointment.cancelledAt        = LocalDateTime.now();
        appointment.persist();

        AuditTrail.logChange(
            userId, userName, "RECEPTIONIST",
            "APPOINTMENT_CANCELLED", "APPOINTMENTS",
            "Appointment",
            String.valueOf(appointmentId),
            oldStatus, "CANCELLED",
            "Cancelled: " + appointment.appointmentNo +
            " | Patient: " + appointment.patientName +
            " | Doctor: " + appointment.doctorName +
            " | Date: " + appointment.appointmentDate +
            " | Reason: " + reason +
            " | Refund eligible: " + eligibleForRefund +
            " | By: " + userName);

        // Cancellation SMS
        String dateStr = appointment.appointmentDate
            .format(DateTimeFormatter
                .ofPattern("dd MMM yyyy"));
        String timeStr = appointment.appointmentTime
            .format(DateTimeFormatter
                .ofPattern("hh:mm a"));

        try {
            smsService.sendCancellationNotice(
                appointment.patientPhone,
                appointment.patientName,
                appointment.doctorName,
                dateStr, timeStr);
        } catch (Exception e) {
            System.err.println(
                "[SMS FAILED] Cancellation SMS: " +
                e.getMessage());
        }

        return appointment;
    }

    // ── Mark no-shows for a date ──────────────────────
    @Transactional
    public int markNoShows(LocalDate date,
            Long userId, String userName) {

        List<Appointment> toMark = new ArrayList<>();
        toMark.addAll(Appointment.findByDateAndStatus(
            date, "SCHEDULED"));
        toMark.addAll(Appointment.findByDateAndStatus(
            date, "CONFIRMED"));

        int count = 0;
        for (Appointment a : toMark) {
            a.status = "NO_SHOW";
            a.persist();
            count++;

            // Send no-show SMS
            try {
                String dateStr = a.appointmentDate
                    .format(DateTimeFormatter
                        .ofPattern("dd MMM yyyy"));
                smsService.send(a.patientPhone,
                    "Dear " + a.patientName +
                    ", we missed you for your appointment " +
                    "with " + a.doctorName +
                    " on " + dateStr +
                    ". Call Cedar Hospital to reschedule.");
            } catch (Exception e) {
                System.err.println(
                    "[SMS FAILED] No-show SMS: " +
                    e.getMessage());
            }
        }

        if (count > 0) {
            AuditTrail.log(
                userId, userName, "RECEPTIONIST",
                "NO_SHOWS_MARKED", "APPOINTMENTS",
                "Appointment", "BATCH",
                count + " no-shows marked for " +
                date + " | By: " + userName);
        }

        return count;
    }

    // ── Queries ───────────────────────────────────────
    public List<DoctorSchedule> getAllSchedules() {
        return DoctorSchedule.findAllActive();
    }

    public List<Appointment> getTodaysAppointments() {
        return Appointment.findByDate(LocalDate.now());
    }

    public List<Appointment> getAppointmentsByDate(
            LocalDate date) {
        return Appointment.findByDate(date);
    }

    public List<Appointment> getByPatient(Long patientId) {
        return Appointment.findByPatient(patientId);
    }

    public List<Appointment> getByPhone(String phone) {
        return Appointment.findByPhone(phone);
    }

    public List<Appointment> getTodayArrived() {
        return Appointment.findTodayArrived();
    }
}