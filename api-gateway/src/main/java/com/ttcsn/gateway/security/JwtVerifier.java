package com.ttcsn.gateway.security;

import com.ttcsn.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Verify access token phát hành bởi user-service.
 *
 * <p>Cùng thuật toán HS256, cùng issuer và cùng claim layout:
 * sub = userId, role = CUSTOMER|ADMIN, type = access|refresh.
 * Refresh token bị từ chối vì filter chỉ chấp nhận type = "access".
 *
 * <p>Không log token. Mọi lỗi verify đều ném ra ngoài để filter trả 401 chung.
 */
@Component
public class JwtVerifier {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final Set<String> ALLOWED_ROLES = Set.of("CUSTOMER", "ADMIN");

    private final SecretKey key;
    private final String issuer;

    public JwtVerifier(GatewaySecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.jwtIssuer();
    }

    public VerifiedUser verify(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);

        // Chỉ chấp nhận đúng thuật toán user-service dùng, không để token HS512/HS384 lọt qua.
        if (!Jwts.SIG.HS256.getId().equals(jws.getHeader().getAlgorithm())) {
            throw new IllegalArgumentException("Unsupported JWT algorithm");
        }

        Claims claims = jws.getPayload();
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new IllegalArgumentException("Not an access token");
        }

        String role = claims.get(CLAIM_ROLE, String.class);
        if (role == null || !ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Invalid role");
        }

        return new VerifiedUser(Long.valueOf(claims.getSubject()), role);
    }

    public record VerifiedUser(Long userId, String role) {
    }
}
