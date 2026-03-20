package ke.cedar.hmis.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends PanacheEntity {

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "user_name")
    public String userName;

    @Column(nullable = false)
    public String action;

    public String resource;

    @Column(name = "resource_id")
    public String resourceId;

    public String details;

    @Column(name = "ip_address")
    public String ipAddress;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static void log(Long userId, String userName,
            String action, String resource, String details) {
        AuditLog entry   = new AuditLog();
        entry.userId     = userId;
        entry.userName   = userName;
        entry.action     = action;
        entry.resource   = resource;
        entry.details    = details;
        entry.persist();
    }
}