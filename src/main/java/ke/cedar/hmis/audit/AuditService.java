package ke.cedar.hmis.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AuditService {

    @Transactional
    public void log(Long userId, String userName,
            String userRole, String action,
            String module, String resource,
            String resourceId, String description) {

        AuditTrail.log(userId, userName, userRole,
                action, module, resource,
                resourceId, description);
    }

    @Transactional
    public void logChange(Long userId, String userName,
            String userRole, String action,
            String module, String resource,
            String resourceId, String oldValue,
            String newValue, String description) {

        AuditTrail.logChange(userId, userName, userRole,
                action, module, resource, resourceId,
                oldValue, newValue, description);
    }

    public List<AuditTrail> getByUser(Long userId) {
        return AuditTrail.findByUser(userId);
    }

    public List<AuditTrail> getByModule(String module) {
        return AuditTrail.findByModule(module);
    }

    public List<AuditTrail> getByAction(String action) {
        return AuditTrail.findByAction(action);
    }

    public List<AuditTrail> getByResource(
            String resource, String resourceId) {
        return AuditTrail.findByResource(resource, resourceId);
    }

    public List<AuditTrail> getByDateRange(
            LocalDateTime from, LocalDateTime to) {
        return AuditTrail.findByDateRange(from, to);
    }

    public List<AuditTrail> getByUserAndDateRange(
            Long userId, LocalDateTime from, LocalDateTime to) {
        return AuditTrail.findByUserAndDateRange(
                userId, from, to);
    }

    public List<AuditTrail> getFailedLogins() {
        return AuditTrail.findFailedLogins();
    }

    public List<AuditTrail> getSensitiveActions() {
        return AuditTrail.findSensitiveActions();
    }

    public List<AuditTrail> getAll() {
        return AuditTrail.find(
                "ORDER BY createdAt DESC").list();
    }
}