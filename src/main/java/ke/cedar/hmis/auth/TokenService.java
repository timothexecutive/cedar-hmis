package ke.cedar.hmis.auth;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    public String generateToken(User user) {
        return Jwt.issuer("hmis.cedarhospital.co.ke")
                .subject(String.valueOf(user.id))
                .groups(Set.of(user.role.name()))
                .claim("userId",   user.id)
                .claim("fullName", user.fullName)
                .claim("staffNo",  user.staffNo)
                .claim("role",     user.role.name())
                .claim("email",    user.email)
                .expiresIn(Duration.ofHours(8))
                .sign();
    }
}