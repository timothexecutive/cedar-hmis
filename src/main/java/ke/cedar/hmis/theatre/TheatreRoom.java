package ke.cedar.hmis.theatre;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "theatre_rooms")
public class TheatreRoom extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(unique = true, nullable = false)
    public String code;

    @Column(name = "room_type")
    public String roomType = "GENERAL";

    public String status = "AVAILABLE";
    public String floor;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "is_active")
    public Boolean isActive = true;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static List<TheatreRoom> findAllActive() {
        return find("isActive = true ORDER BY name ASC")
                .list();
    }

    public static TheatreRoom findByCode(String code) {
        return find("code", code).firstResult();
    }

    public static List<TheatreRoom> findAvailable() {
        return find("isActive = true AND status = 'AVAILABLE' " +
                    "ORDER BY name ASC").list();
    }
}