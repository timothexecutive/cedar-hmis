package ke.cedar.hmis.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class AuthService {

    @Transactional
    public User login(String email, String password) {
        User user = User.findByEmail(email);

        if (user == null || !user.isActive) {
            throw new WebApplicationException(
                Response.status(401)
                    .entity("{\"error\":\"Invalid email or password\"}")
                    .build());
        }

        if (user.isLocked()) {
            throw new WebApplicationException(
                Response.status(423)
                    .entity("{\"error\":\"Account locked. Contact admin\"}")
                    .build());
        }

        if (!BCrypt.checkpw(password, user.passwordHash)) {
            user.failedAttempts++;
            if (user.failedAttempts >= 3) {
                user.lockedUntil = LocalDateTime.now().plusMinutes(30);
            }
            user.persist();
            throw new WebApplicationException(
                Response.status(401)
                    .entity("{\"error\":\"Invalid email or password\"}")
                    .build());
        }

        user.failedAttempts = 0;
        user.lockedUntil    = null;
        user.lastLogin      = LocalDateTime.now();
        user.persist();
        return user;
    }

    @Transactional
    public User createUser(User request, String plainPassword) {
        if (User.findByEmail(request.email) != null) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Email already exists\"}")
                    .build());
        }
        request.passwordHash = BCrypt.hashpw(plainPassword,
            BCrypt.gensalt(12));
        long count      = User.count() + 1;
        request.staffNo = String.format("STF-%04d", count);
        request.persist();
        return request;
    }

    @Transactional
    public void changePassword(Long userId,
            String oldPassword, String newPassword) {
        User user = User.findById(userId);
        if (user == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"User not found\"}")
                    .build());
        }
        if (!BCrypt.checkpw(oldPassword, user.passwordHash)) {
            throw new WebApplicationException(
                Response.status(401)
                    .entity("{\"error\":\"Current password is incorrect\"}")
                    .build());
        }
        user.passwordHash = BCrypt.hashpw(newPassword,
            BCrypt.gensalt(12));
        user.persist();
    }

    public List<User> getAllUsers() {
        return User.listAll();
    }
}