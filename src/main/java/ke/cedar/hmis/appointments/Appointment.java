package ke.cedar.hmis.appointments;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "appointments")
public class Appointment extends PanacheEntity {

    @Column(name = "appointment_no",
            unique = true, nullable = false)
    public String appointmentNo;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    public DoctorSchedule schedule;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    public Patient patient;

    @Column(name = "patient_name", nullable = false)
    public String patientName;

    @Column(name = "patient_phone", nullable = false)
    public String patientPhone;

    @Column(name = "patient_national_id")
    public String patientNationalId;

    @Column(name = "doctor_name", nullable = false)
    public String doctorName;

    public String department;

    @Column(name = "appointment_date", nullable = false)
    public LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    public LocalTime appointmentTime;

    @Column(columnDefinition = "TEXT")
    public String reason;

    public String status = "SCHEDULED";

    @Column(name = "payment_type", nullable = false)
    public String paymentType = "CASH";

    @Column(name = "consultation_fee")
    public BigDecimal consultationFee;

    @Column(name = "mpesa_checkout_id")
    public String mpesaCheckoutId;

    @Column(name = "mpesa_receipt")
    public String mpesaReceipt;

    @Column(name = "payment_verified")
    public Boolean paymentVerified = false;

    @Column(name = "insurance_provider")
    public String insuranceProvider;

    @Column(name = "insurance_member_no")
    public String insuranceMemberNo;

    @Column(name = "insurance_verified")
    public Boolean insuranceVerified = false;

    @Column(name = "cancellation_reason",
            columnDefinition = "TEXT")
    public String cancellationReason;

    @Column(name = "cancelled_at")
    public LocalDateTime cancelledAt;

    @Column(name = "cancellation_deadline")
    public LocalDateTime cancellationDeadline;

    @Column(name = "refund_issued")
    public Boolean refundIssued = false;

    @Column(name = "refund_ref")
    public String refundRef;

    @Column(name = "arrived_at")
    public LocalDateTime arrivedAt;

    @Column(name = "marked_arrived_by")
    public String markedArrivedBy;

    @ManyToOne
    @JoinColumn(name = "visit_id")
    public Visit visit;

    @Column(name = "booking_source")
    public String bookingSource = "RECEPTION";

    @Column(name = "booked_by")
    public String bookedBy;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Set cancellation deadline — 2hrs before appointment
        if (appointmentDate != null &&
                appointmentTime != null) {
            cancellationDeadline = appointmentDate
                .atTime(appointmentTime)
                .minusHours(2);
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean canBeCancelled() {
        if (cancellationDeadline == null) return true;
        return LocalDateTime.now()
            .isBefore(cancellationDeadline);
    }

    // ── Queries ───────────────────────────────────────
    public static List<Appointment> findByDate(
            LocalDate date) {
        return find("appointmentDate = ?1 " +
                    "ORDER BY appointmentTime ASC",
                date).list();
    }

    public static List<Appointment> findByDateAndStatus(
            LocalDate date, String status) {
        return find("appointmentDate = ?1 " +
                    "AND status = ?2 " +
                    "ORDER BY appointmentTime ASC",
                date, status).list();
    }

    public static List<Appointment> findByPatient(
            Long patientId) {
        return find("patient.id = ?1 " +
                    "ORDER BY appointmentDate DESC, " +
                    "appointmentTime DESC",
                patientId).list();
    }

    public static List<Appointment> findByDoctor(
            Long scheduleId, LocalDate date) {
        return find("schedule.id = ?1 " +
                    "AND appointmentDate = ?2 " +
                    "ORDER BY appointmentTime ASC",
                scheduleId, date).list();
    }

    public static List<Appointment> findTodayArrived() {
        return find("appointmentDate = ?1 " +
                    "AND status = 'ARRIVED' " +
                    "ORDER BY arrivedAt ASC",
                LocalDate.now()).list();
    }

    public static boolean slotTaken(Long scheduleId,
            LocalDate date, LocalTime time) {
        return count("schedule.id = ?1 " +
                     "AND appointmentDate = ?2 " +
                     "AND appointmentTime = ?3 " +
                     "AND status NOT IN " +
                     "('CANCELLED', 'NO_SHOW')",
                scheduleId, date, time) > 0;
    }

    public static List<Appointment> findByPhone(
            String phone) {
        return find("patientPhone = ?1 " +
                    "ORDER BY appointmentDate DESC",
                phone).list();
    }

    public static List<Appointment> findNoShows(
            LocalDate date) {
        return find("appointmentDate = ?1 " +
                    "AND status = 'SCHEDULED' " +
                    "OR (appointmentDate = ?1 " +
                    "AND status = 'CONFIRMED')",
                date).list();
    }
}