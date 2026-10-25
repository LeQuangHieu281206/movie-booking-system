package com.ttcsn.user.security.impl;

import com.ttcsn.user.config.JwtProperties;
import com.ttcsn.user.entity.User;
import com.ttcsn.user.entity.enums.UserRole;
import com.ttcsn.user.exception.InvalidTokenException;
import com.ttcsn.user.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test JwtService thuần JUnit 5, không cần Spring context.
 *
 * @author quanghieu
 */
class JwtServiceTest {

    /** 47 ký tự: chứng minh thuật toán cố định HS256 không phụ thuộc độ dài key. */
    private static final String SECRET = "unit-test-secret-with-at-least-32-characters-ok!";
    private static final String ISSUER = "mtbs-user-service";

    private static JwtService service;
    private static SecretKey key;

    @BeforeAll
    static void setUp() {
        service = new JwtServiceImpl(new JwtProperties(SECRET, 900_000L, 604_800_000L, ISSUER));
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private static User user(Long id, String role) {
        User u = new User();
        u.setUserId(id);
        u.setRole(role == null ? null : UserRole.valueOf(role));
        u.setFullName("Nguoi Thu nghiem");
        u.setEmail("a@b.com");
        return u;
    }

    @Test
    void createThenParseAccessClaims() {
        String access = service.generateAccessToken(user(1L, "CUSTOMER"));
        Claims c = service.parseAndVerify(access);

        assertEquals("1", c.getSubject());
        assertEquals(Long.valueOf(1L), service.extractUserId(c));
        assertEquals("a@b.com", service.extractEmail(c));
        assertEquals("CUSTOMER", service.extractRole(c));
        assertEquals(JwtService.CLAIM_TYPE_ACCESS, c.get(JwtService.CLAIM_TYPE, String.class));
        assertEquals(ISSUER, c.getIssuer());
        assertNotNull(c.getIssuedAt());
        assertNotNull(c.getExpiration());
        long accessTtl = c.getExpiration().getTime() - c.getIssuedAt().getTime();
        assertEquals(900_000L, accessTtl, 1500);

        String refresh = service.generateRefreshToken(user(1L, "CUSTOMER"));
        Claims rc = service.parseAndVerify(refresh);
        assertEquals(JwtService.CLAIM_TYPE_REFRESH, rc.get(JwtService.CLAIM_TYPE, String.class));
        assertNotNull(rc.getId(), "refresh token phải có jti");
        long refreshTtl = rc.getExpiration().getTime() - rc.getIssuedAt().getTime();
        assertEquals(604_800_000L, refreshTtl, 1500);
    }

    @Test
    void refreshTokenCannotBeUsedAsAccess() {
        String refresh = service.generateRefreshToken(user(1L, "CUSTOMER"));
        Claims rc = service.parseAndVerify(refresh);
        assertThrows(InvalidTokenException.class,
                () -> service.requireTokenType(rc, JwtService.CLAIM_TYPE_ACCESS));
    }

    @Test
    void expiredTokenRejected() {
        Date now = new Date();
        String expired = Jwts.builder()
                .subject("1")
                .claim(JwtService.CLAIM_TYPE, JwtService.CLAIM_TYPE_ACCESS)
                .issuer(ISSUER)
                .issuedAt(new Date(now.getTime() - 10_000))
                .expiration(new Date(now.getTime() - 100))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThrows(InvalidTokenException.class, () -> service.parseAndVerify(expired));
    }

    @Test
    void tamperedPayloadRejected() {
        String token = service.generateAccessToken(user(1L, "CUSTOMER"));
        String[] parts = token.split("\\.");
        // Sửa 1 ký tự trong payload nhưng giữ nguyên chữ ký
        String swapped = "B".equals(parts[1].substring(0, 1)) ? "C" : "B";
        String tampered = parts[0] + "." + swapped + parts[1].substring(1) + "." + parts[2];
        assertThrows(InvalidTokenException.class, () -> service.parseAndVerify(tampered));
    }

    @Test
    void wrongSecretRejected() {
        SecretKey otherKey = Keys.hmacShaKeyFor("another-unit-test-secret-32-char!!".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("1")
                .claim(JwtService.CLAIM_TYPE, JwtService.CLAIM_TYPE_ACCESS)
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();
        assertThrows(InvalidTokenException.class, () -> service.parseAndVerify(token));
    }

    @Test
    void wrongIssuerRejected() {
        String token = Jwts.builder()
                .subject("1")
                .claim(JwtService.CLAIM_TYPE, JwtService.CLAIM_TYPE_ACCESS)
                .issuer("evil-issuer")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThrows(InvalidTokenException.class, () -> service.parseAndVerify(token));
    }

    @Test
    void nullRoleRejected() {
        assertThrows(IllegalStateException.class,
                () -> service.generateAccessToken(user(1L, null)));
    }
}
