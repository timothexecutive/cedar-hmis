package ke.cedar.hmis.audit;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "audit_trail")
public class AuditTrail extends PanacheEntity {

    @Column(name = "user_id")
    public Long userId;

    @Column(name = "user_name")
    public String userName;

    @Column(name = "user_role")
    public String userRole;

    @Column(nullable = false)
    public String action;

    @Column(nullable = false)
    public String module;

    public String resource;

    @Column(name = "resource_id")
    public String resourceId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    public String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    public String newValue;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "ip_address")
    public String ipAddress;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── The main logging method ───────────────────────
    public static void log(
            Long userId,
            String userName,
            String userRole,
            String action,
            String module,
            String resource,
            String resourceId,
            String description) {

        AuditTrail entry   = new AuditTrail();
        entry.userId       = userId;
        entry.userName     = userName;
        entry.userRole     = userRole;
        entry.action       = action;
        entry.module       = module;
        entry.resource     = resource;
        entry.resourceId   = resourceId;
        entry.description  = description;
        entry.persist();
    }

    // ── Log with before/after values (for changes) ────
    public static void logChange(
            Long userId,
            String userName,
            String userRole,
            String action,
            String module,
            String resource,
            String resourceId,
            String oldValue,
            String newValue,
            String description) {

        AuditTrail entry   = new AuditTrail();
        entry.userId       = userId;
        entry.userName     = userName;
        entry.userRole     = userRole;
        entry.action       = action;
        entry.module       = module;
        entry.resource     = resource;
        entry.resourceId   = resourceId;
        entry.oldValue     = oldValue;
        entry.newValue     = newValue;
        entry.description  = description;
        entry.persist();
    }

    // ── Queries ───────────────────────────────────────
    public static List<AuditTrail> findByUser(Long userId) {
        return find("userId = ?1 ORDER BY createdAt DESC",
                userId).list();
    }

    public static List<AuditTrail> findByModule(String module) {
        return find("module = ?1 ORDER BY createdAt DESC",
                module).list();
    }

    public static List<AuditTrail> findByAction(String action) {
        return find("action = ?1 ORDER BY createdAt DESC",
                action).list();
    }

    public static List<AuditTrail> findByResource(
            String resource, String resourceId) {
        return find("resource = ?1 AND resourceId = ?2 " +
                    "ORDER BY createdAt DESC",
                resource, resourceId).list();
    }

    public static List<AuditTrail> findByDateRange(
            LocalDateTime from, LocalDateTime to) {
        return find("createdAt >= ?1 AND createdAt <= ?2 " +
                    "ORDER BY createdAt DESC",
                from, to).list();
    }

    public static List<AuditTrail> findByUserAndDateRange(
            Long userId, LocalDateTime from, LocalDateTime to) {
        return find("userId = ?1 AND createdAt >= ?2 " +
                    "AND createdAt <= ?3 ORDER BY createdAt DESC",
                userId, from, to).list();
    }

    public static List<AuditTrail> findFailedLogins() {
        return find("action = 'LOGIN_FAILED' " +
                    "ORDER BY createdAt DESC").list();
    }

    public static List<AuditTrail> findSensitiveActions() {
        return find("action IN ('INVOICE_VOIDED', " +
                    "'USER_CREATED', 'PASSWORD_CHANGED', " +
                    "'DRUG_DISPENSED', 'RESULT_VERIFIED') " +
                    "ORDER BY createdAt DESC").list();
    }
}