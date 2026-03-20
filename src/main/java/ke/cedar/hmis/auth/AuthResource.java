package ke.cedar.hmis.auth;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject AuthService  authService;
    @Inject TokenService tokenService;

    // ── TEMP: generate hash — remove before production ─
    @GET
    @Path("/hash/{password}")
    @Produces(MediaType.TEXT_PLAIN)
    public String hashPassword(@PathParam("password") String password) {
        return org.mindrot.jbcrypt.BCrypt.hashpw(
            password, org.mindrot.jbcrypt.BCrypt.gensalt(12));
    }

    // ── LOGIN — public, no token needed ───────────────
    @POST
    @Path("/login")
    @Transactional
    public Response login(Map<String, String> body) {
        User user    = authService.login(
            body.get("email"),
            body.get("password"));
        String token = tokenService.generateToken(user);
        return Response.ok(Map.of(
            "token",    token,
            "userId",   user.id,
            "fullName", user.fullName,
            "role",     user.role.name(),
            "staffNo",  user.staffNo
        )).build();
    }

    // ── CREATE user — admin only ──────────────────────
    @POST
    @Path("/users")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response createUser(Map<String, String> body) {
        User user       = new User();
        user.fullName   = body.get("fullName");
        user.email      = body.get("email");
        user.phone      = body.get("phone");
        user.role       = Role.valueOf(body.get("role"));
        user.department = body.get("department");

        User saved = authService.createUser(user, body.get("password"));

        return Response.status(201).entity(Map.of(
            "id",         saved.id,
            "staffNo",    saved.staffNo,
            "fullName",   saved.fullName,
            "email",      saved.email,
            "role",       saved.role.name(),
            "department", saved.department != null ? saved.department : ""
        )).build();
    }

    // ── GET all users — admin only ────────────────────
    @GET
    @Path("/users")
    @RolesAllowed({"ADMIN"})
    public Response getAllUsers() {
        List<User> users = authService.getAllUsers();
        List<Map<String, Object>> safeUsers = users.stream()
            .map(u -> Map.<String, Object>of(
                "id",         u.id,
                "staffNo",    u.staffNo,
                "fullName",   u.fullName,
                "email",      u.email != null ? u.email : "",
                "role",       u.role.name(),
                "department", u.department != null ? u.department : "",
                "isActive",   u.isActive
            ))
            .collect(Collectors.toList());
        return Response.ok(safeUsers).build();
    }

    // ── CHANGE password — any logged in user ──────────
    @PUT
    @Path("/users/{userId}/password")
    @Transactional
    @RolesAllowed({"ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST",
                   "CASHIER", "LAB_TECH", "PHARMACIST",
                   "CLINICAL_OFFICER", "RADIOLOGIST", "CEO"})
    public Response changePassword(
            @PathParam("userId") Long userId,
            Map<String, String> body) {
        authService.changePassword(
            userId,
            body.get("oldPassword"),
            body.get("newPassword"));
        return Response.ok(
            Map.of("message", "Password changed successfully")
        ).build();
    }
}
