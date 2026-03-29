package ke.cedar.hmis.theatre;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.ipd.Admission;
import ke.cedar.hmis.maternity.MaternityAdmission;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "surgery_bookings")
public class SurgeryBooking extends PanacheEntity {

    @Column(name = "booking_no", unique = true,
            nullable = false)
    public String bookingNo;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @ManyToOne
    @JoinColumn(name = "theatre_room_id", nullable = false)
    public TheatreRoom theatreRoom;

    @ManyToOne
    @JoinColumn(name = "ipd_admission_id")
    public Admission ipdAdmission;

    @ManyToOne
    @JoinColumn(name = "maternity_id")
    public MaternityAdmission maternityAdmission;

    @Column(name = "surgery_type", nullable = false)
    public String surgeryType;

    @Column(name = "surgery_category")
    public String surgeryCategory = "ELECTIVE";

    @Column(columnDefinition = "TEXT")
    public String diagnosis;

    @Column(name = "planned_date", nullable = false)
    public LocalDate plannedDate;

    @Column(name = "planned_start_time")
    public LocalTime plannedStartTime;

    @Column(name = "planned_duration")
    public Integer plannedDuration;

    @Column(name = "lead_surgeon")
    public String leadSurgeon;

    @Column(name = "assistant_surgeon")
    public String assistantSurgeon;

    public String anaesthetist;

    @Column(name = "scrub_nurse")
    public String scrubNurse;

    @Column(name = "circulating_nurse")
    public String circulatingNurse;

    @Column(name = "anaesthesia_type")
    public String anaesthesiaType;

    @Column(name = "special_equipment",
            columnDefinition = "TEXT")
    public String specialEquipment;

    @Column(name = "blood_ordered")
    public Boolean bloodOrdered = false;

    @Column(name = "blood_units")
    public Integer bloodUnits = 0;

    @Column(name = "consent_signed")
    public Boolean consentSigned = false;

    public String status = "SCHEDULED";

    @Column(name = "cancellation_reason",
            columnDefinition = "TEXT")
    public String cancellationReason;

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
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Queries ───────────────────────────────────────
    public static List<SurgeryBooking> findByDate(
            LocalDate date) {
        return find("plannedDate = ?1 " +
                    "ORDER BY plannedStartTime ASC",
                date).list();
    }

    public static List<SurgeryBooking> findByStatus(
            String status) {
        return find("status = ?1 " +
                    "ORDER BY plannedDate ASC, " +
                    "plannedStartTime ASC", status).list();
    }

    public static List<SurgeryBooking> findByPatient(
            Long patientId) {
        return find("patient.id = ?1 " +
                    "ORDER BY plannedDate DESC",
                patientId).list();
    }

    public static List<SurgeryBooking> findByRoom(
            Long roomId, LocalDate date) {
        return find("theatreRoom.id = ?1 " +
                    "AND plannedDate = ?2 " +
                    "ORDER BY plannedStartTime ASC",
                roomId, date).list();
    }

    public static List<SurgeryBooking> findByMonth(
            int year, int month) {
        return find("EXTRACT(YEAR FROM plannedDate) = ?1 " +
                    "AND EXTRACT(MONTH FROM plannedDate) = ?2 " +
                    "ORDER BY plannedDate ASC",
                year, month).list();
    }

   public static boolean hasConflict(Long roomId,
        LocalDate date, LocalTime startTime,
        Integer duration) {

    // Get all bookings for this room on this date
    // that are not cancelled or completed
    List<SurgeryBooking> existing = find(
        "theatreRoom.id = ?1 " +
        "AND plannedDate = ?2 " +
        "AND status NOT IN ('CANCELLED', 'COMPLETED')",
        roomId, date).list();

    if (existing.isEmpty()) return false;

    LocalTime newEnd = startTime.plusMinutes(duration);

    for (SurgeryBooking booking : existing) {
        if (booking.plannedStartTime == null ||
                booking.plannedDuration == null) continue;

        LocalTime existingEnd = booking.plannedStartTime
                .plusMinutes(booking.plannedDuration);

        // Check overlap — two bookings overlap if:
        // new starts before existing ends
        // AND new ends after existing starts
        boolean overlaps =
            startTime.isBefore(existingEnd) &&
            newEnd.isAfter(booking.plannedStartTime);

        if (overlaps) return true;
    }

    return false;
}
}