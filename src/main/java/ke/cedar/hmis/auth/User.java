package ke.cedar.hmis.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @Column(name = "staff_no", unique = true, nullable = false)
    public String staffNo;

    @Column(name = "full_name", nullable = false)
    public String fullName;

    @Column(unique = true)
    public String email;

    @Column(nullable = false)
    public String phone;

    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;

    public String department;

    @Column(name = "is_active")
    public boolean isActive = true;

    @Column(name = "failed_attempts")
    public int failedAttempts = 0;

    @Column(name = "locked_until")
    public LocalDateTime lockedUntil;

    @Column(name = "last_login")
    public LocalDateTime lastLogin;

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

    public static User findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public static User findByStaffNo(String staffNo) {
        return find("staffNo", staffNo).firstResult();
    }

    public boolean isLocked() {
        return lockedUntil != null &&
               lockedUntil.isAfter(LocalDateTime.now());
    }
}