package ke.cedar.hmis.audit;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.List;

@Path("/api/audit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditResource {

    @Inject AuditService auditService;

    // ── All logs — admin only ─────────────────────────
    @GET
    @Path("/logs")
    @RolesAllowed({"ADMIN"})
    public List<AuditTrail> getAllLogs() {
        return auditService.getAll();
    }

    // ── Logs by user ──────────────────────────────────
    @GET
    @Path("/logs/user/{userId}")
    @RolesAllowed({"ADMIN"})
    public List<AuditTrail> getByUser(
            @PathParam("userId") Long userId) {
        return auditService.getByUser(userId);
    }

    // ── Logs by module ────────────────────────────────
    @GET
    @Path("/logs/module/{module}")
    @RolesAllowed({"ADMIN"})
    public List<AuditTrail> getByModule(
            @PathParam("module") String module) {
        return auditService.getByModule(module);
    }

    // ── Logs by action ────────────────────────────────
    @GET
    @Path("/logs/action/{action}")
    @RolesAllowed({"ADMIN"})
    public List<AuditTrail> getByAction(
            @PathParam("action") String action) {
        return auditService.getByAction(action);
    }

    // ── Logs for a specific record ────────────────────
    @GET
    @Path("/logs/resource/{resource}/{resourceId}")
    @RolesAllowed({"ADMIN", "DOCTOR", "NURSE"})
    public List<AuditTrail> getByResource(
            @PathParam("resource") String resource,
            @PathParam("resourceId") String resourceId) {
        return auditService.getByResource(
                resource, resourceId);
    }

    // ── Failed login attempts ─────────────────────────
    @GET
    @Path("/logs/failed-logins")
    @RolesAllowed({"ADMIN"})
    public List<AuditTrail> getFailedLogins() {
        return auditService.getFailedLogins();
    }

    // ── Sensitive actions ─────────────────────────────
    @GET
    @Path("/logs/sensitive")
    @RolesAllowed({"ADMIN"})
    public List<AuditTrail> getSensitiveActions() {
        return auditService.getSensitiveActions();
    }

    // ── Date range filter ─────────────────────────────
    @GET
    @Path("/logs/range")
    @RolesAllowed({"ADMIN"})
    public List<AuditTrail> getByDateRange(
            @QueryParam("from") String from,
            @QueryParam("to")   String to) {
        return auditService.getByDateRange(
                LocalDateTime.parse(from),
                LocalDateTime.parse(to));
    }

    // ── User activity in date range ───────────────────
    @GET
    @Path("/logs/user/{userId}/range")
    @RolesAllowed({"ADMIN"})
    public List<AuditTrail> getByUserAndDateRange(
            @PathParam("userId") Long userId,
            @QueryParam("from")  String from,
            @QueryParam("to")    String to) {
        return auditService.getByUserAndDateRange(
                userId,
                LocalDateTime.parse(from),
                LocalDateTime.parse(to));
    }
}