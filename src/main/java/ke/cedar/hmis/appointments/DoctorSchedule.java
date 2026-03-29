package ke.cedar.hmis.appointments;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "doctor_schedules")
public class DoctorSchedule extends PanacheEntity {

    @Column(name = "doctor_name", nullable = false)
    public String doctorName;

    @Column(name = "doctor_email")
    public String doctorEmail;

    public String department;

    @Column(name = "working_days", nullable = false)
    public String workingDays = "MON,TUE,WED,THU,FRI";

    @Column(name = "start_time", nullable = false)
    public LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    public LocalTime endTime;

    @Column(name = "slot_duration_mins", nullable = false)
    public Integer slotDurationMins = 30;

    @Column(name = "max_slots_per_day", nullable = false)
    public Integer maxSlotsPerDay = 16;

    @Column(name = "consultation_fee")
    public BigDecimal consultationFee =
        new BigDecimal("500.00");

    @Column(name = "is_active")
    public Boolean isActive = true;

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

    // ── Check if doctor works on a given day ──────────
    public boolean worksOnDay(String dayCode) {
        if (workingDays == null) return false;
        return Arrays.asList(
            workingDays.split(",")).contains(dayCode);
    }

    // ── Generate time slots for a day ─────────────────
    public List<LocalTime> generateSlots() {
        List<LocalTime> slots = new java.util.ArrayList<>();
        LocalTime current = startTime;
        while (current.isBefore(endTime)) {
            slots.add(current);
            current = current.plusMinutes(slotDurationMins);
            if (slots.size() >= maxSlotsPerDay) break;
        }
        return slots;
    }

    // ── Queries ───────────────────────────────────────
    public static List<DoctorSchedule> findAllActive() {
        return find("isActive = true " +
                    "ORDER BY doctorName ASC").list();
    }

    public static List<DoctorSchedule> findByDepartment(
            String department) {
        return find("department = ?1 " +
                    "AND isActive = true " +
                    "ORDER BY doctorName ASC",
                department).list();
    }
}