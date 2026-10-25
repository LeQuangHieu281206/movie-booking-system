package com.ttcsn.user.security.impl;

import com.ttcsn.user.config.JwtProperties;
import com.ttcsn.user.entity.User;
import com.ttcsn.user.exception.InvalidTokenException;
import com.ttcsn.user.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Sinh va parse JWT bang jjwt 0.12.6 (API moi).
 *
 * - Thuat toan co dinh HS256 qua {@code signWith(key, Jwts.SIG.HS256)}: khong de thu vien
 *   tu suy thuat toan theo do dai key (32 byte se ra HS256, du dai hon thi ra HS384...).
 *   Nho vay chu ky khong phu thuoc do dai secret; cac service khac cung chi dung HS256.
 * - Refresh token them jti (UUID) cho muc dich revoke theo jti neu sau nay chuyen stateful.
 *
 * @author quanghieu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private static final String GENERIC_TOKEN_ERROR = "Token khong hop le hoac da het han";

    private final JwtProperties jwtProperties;

    @Override
    public String generateAccessToken(User user) {
        if (user.getRole() == null) {
            throw new IllegalStateException(
                    "Khong the cap access token: user id=" + user.getUserId() + " chua co role hop le");
        }
        return buildToken(user, CLAIM_TYPE_ACCESS, jwtProperties.accessExpirationMs(), false);
    }

    @Override
    public String generateRefreshToken(User user) {
        if (user.getRole() == null) {
            throw new IllegalStateException(
                    "Khong the cap refresh token: user id=" + user.getUserId() + " chua co role hop le");
        }
        return buildToken(user, CLAIM_TYPE_REFRESH, jwtProperties.refreshExpirationMs(), true);
    }

    @Override
    public Claims parseAndVerify(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(jwtProperties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token sai chu ky, cau truc, issuer hoac da het han");
            throw new InvalidTokenException(GENERIC_TOKEN_ERROR);
        }
    }

    @Override
    public Claims requireTokenType(Claims claims, String expectedType) {
        String type = claims.get(CLAIM_TYPE, String.class);
        if (type == null || !type.equals(expectedType)) {
            log.debug("Sai loai token (expect {}, got {})", expectedType, type);
            throw new InvalidTokenException(GENERIC_TOKEN_ERROR);
        }
        return claims;
    }

    @Override
    public long accessExpirationMs() {
        return jwtProperties.accessExpirationMs();
    }

    @Override
    public Long extractUserId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new InvalidTokenException(GENERIC_TOKEN_ERROR);
        }
    }

    @Override
    public String extractEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    @Override
    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    // ---- helpers ----

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(User user, String type, long ttlMs, boolean withJti) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs));
        if (withJti) {
            builder.id(UUID.randomUUID().toString());
        }
        return builder.signWith(signingKey(), Jwts.SIG.HS256).compact();
    }
}
