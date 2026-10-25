package com.ttcsn.gateway.security;

import com.ttcsn.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho gateway filter — không cần Spring context/DB.
 */
class GatewayJwtFilterTest {

    private static final String SECRET = "gateway-unit-test-secret-32-characters-minimum";
    private static final String ISSUER = "mtbs-user-service";
    private static final String INTERNAL_TOKEN = "internal-unit-test-token-32-characters-minimum";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private GatewayJwtFilter filter;

    @BeforeEach
    void setUp() {
        GatewaySecurityProperties properties =
                new GatewaySecurityProperties(SECRET, ISSUER, INTERNAL_TOKEN);
        properties.afterPropertiesSet();
        filter = new GatewayJwtFilter(new JwtVerifier(properties), properties);
    }

    @Test
    void requestWithoutTokenIsUnauthorized() {
        Result result = run(MockServerHttpRequest.post("/api/createBooking").build());

        assertUnauthorized(result);
    }

    @Test
    void tokenWithWrongSignatureIsUnauthorized() {
        Result result = run(MockServerHttpRequest.post("/api/createBooking")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER") + "x")
                .build());

        assertUnauthorized(result);
    }

    @Test
    void expiredTokenIsUnauthorized() {
        String expired = token("access", "CUSTOMER", Instant.now().minusSeconds(5));
        Result result = run(MockServerHttpRequest.post("/api/createBooking")
                .header("Authorization", "Bearer " + expired)
                .build());

        assertUnauthorized(result);
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() {
        String refresh = token("refresh", "CUSTOMER", Instant.now().plusSeconds(3600));
        Result result = run(MockServerHttpRequest.post("/api/createBooking")
                .header("Authorization", "Bearer " + refresh)
                .build());

        assertUnauthorized(result);
    }

    @Test
    void customerCannotCallAdminEndpoint() {
        Result result = run(MockServerHttpRequest.post("/api/createMovie")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertForbidden(result);
    }

    @Test
    void customerCannotCallLegacyUserAdminEndpoint() {
        Result result = run(MockServerHttpRequest.get("/api/getUsers")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertForbidden(result);
    }

    @Test
    void rawPathVariantsArePreservedByTestRequest() {
        assertEquals("/api/getMovies%2F..%2FcreateMovie",
                rawRequest(HttpMethod.GET, "/api/getMovies%2F..%2FcreateMovie").build().getURI().getRawPath());
        assertEquals("/api/getMovies/%2e%2e/createMovie",
                rawRequest(HttpMethod.GET, "/api/getMovies/%2e%2e/createMovie").build().getURI().getRawPath());
        assertEquals("/api/createMovie;jsessionid=abc",
                rawRequest(HttpMethod.POST, "/api/createMovie;jsessionid=abc").build().getURI().getRawPath());
        assertEquals("/api//createMovie",
                rawRequest(HttpMethod.POST, "/api//createMovie").build().getURI().getRawPath());
    }

    @Test
    void customerCannotBypassAdminWithTrailingSlash() {
        Result result = run(rawRequest(HttpMethod.POST, "/api/createMovie/")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertForbidden(result);
    }

    @Test
    void customerCannotBypassAdminWithDoubleSlash() {
        Result result = run(rawRequest(HttpMethod.POST, "/api//createMovie")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertNotFound(result);
    }

    @Test
    void customerCannotBypassAdminWithDifferentCase() {
        Result result = run(rawRequest(HttpMethod.POST, "/API/CreateMovie")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertForbidden(result);
    }

    @Test
    void customerCannotBypassAdminWithEncodedSlash() {
        Result result = run(rawRequest(HttpMethod.GET, "/api/getMovies%2F..%2FcreateMovie")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertNotFound(result);
    }

    @Test
    void customerCannotBypassAdminWithEncodedDot() {
        Result result = run(rawRequest(HttpMethod.GET, "/api/getMovies/%2e%2e/createMovie")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertNotFound(result);
    }

    @Test
    void customerCannotBypassAdminWithMatrixParameter() {
        Result result = run(rawRequest(HttpMethod.POST, "/api/createMovie;jsessionid=abc")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertNotFound(result);
    }

    @Test
    void customerCannotBypassAdminWithHttpMethodOverrideHeader() {
        Result result = run(MockServerHttpRequest.get("/api/deleteMovie")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .header("X-HTTP-Method-Override", "DELETE")
                .build());

        assertForbidden(result);
    }

    @Test
    void customerCannotBypassAdminWithMethodOverrideHeader() {
        Result result = run(MockServerHttpRequest.get("/api/deleteMovie")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .header("x-method-override", "DELETE")
                .build());

        assertForbidden(result);
    }

    @Test
    void customerCannotReachUnmatchedPathByDefault() {
        Result result = run(MockServerHttpRequest.get("/api/not-declared")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertForbidden(result);
    }

    @Test
    void adminCanReachUnmatchedPathByDefault() {
        Result result = run(MockServerHttpRequest.get("/api/not-declared")
                .header("Authorization", "Bearer " + accessToken("ADMIN"))
                .build());

        assertNotNull(result.forwarded(), "Mặc định ADMIN được đi tiếp; downstream quyết định 404");
    }

    @Test
    void publicGetWithTrailingSlashIsStillPublic() {
        Result result = run(rawRequest(HttpMethod.GET, "/api/getMovies/").build());

        assertNotNull(result.forwarded(), "Một dấu / ở cuối được chuẩn hóa");
    }

    @Test
    void adminCanCallAdminEndpoint() {
        Result result = run(MockServerHttpRequest.post("/api/createMovie")
                .header("Authorization", "Bearer " + accessToken("ADMIN"))
                .build());

        assertNotNull(result.forwarded(), "ADMIN phải được đi tiếp");
        assertEquals("42", result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.USER_ID_HEADER));
        assertEquals("ADMIN", result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.USER_ROLE_HEADER));
        assertEquals(INTERNAL_TOKEN,
                result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.INTERNAL_TOKEN_HEADER));
    }

    @Test
    void customerCannotListAllBookings() {
        Result result = run(MockServerHttpRequest.get("/api/getBookings")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertForbidden(result);
    }

    @Test
    void customerCannotListBookingsByShowtime() {
        Result result = run(MockServerHttpRequest.get("/api/getBookingsByShowtimeId?showtimeId=10")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertForbidden(result);
    }

    @Test
    void adminCanListAllBookings() {
        Result result = run(MockServerHttpRequest.get("/api/getBookings")
                .header("Authorization", "Bearer " + accessToken("ADMIN"))
                .build());

        assertNotNull(result.forwarded());
    }

    @Test
    void customerCanReadOwnBookingThroughGateway() {
        Result result = run(MockServerHttpRequest.get("/api/getBookingById?id=1")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertNotNull(result.forwarded(), "Ownership được booking-service kiểm tra");
    }

    @Test
    void clientSuppliedIdentityHeadersAreReplaced() {
        Result result = run(MockServerHttpRequest.post("/api/createBooking")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .header("X-User-Id", "999")
                .header("X-User-Role", "ADMIN")
                .header("X-Internal-Token", "fake-internal-token")
                .build());

        assertNotNull(result.forwarded(), "CUSTOMER phải được đi tiếp với booking");
        assertEquals("42", result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.USER_ID_HEADER));
        assertEquals("CUSTOMER",
                result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.USER_ROLE_HEADER));
        assertEquals(INTERNAL_TOKEN,
                result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.INTERNAL_TOKEN_HEADER));
    }

    @Test
    void publicGetNeedsNoTokenAndStripsSpoofedHeaders() {
        Result result = run(MockServerHttpRequest.get("/api/getMovies")
                .header("X-User-Id", "999")
                .header("X-User-Role", "ADMIN")
                .header("X-Internal-Token", "fake-internal-token")
                .build());

        assertNotNull(result.forwarded(), "GET public phải đi tiếp không cần token");
        assertNull(result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.USER_ID_HEADER));
        assertNull(result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.USER_ROLE_HEADER));
        assertEquals(INTERNAL_TOKEN,
                result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.INTERNAL_TOKEN_HEADER));
    }

    @Test
    void publicGetRuleDoesNotWhitelistPost() {
        Result result = run(MockServerHttpRequest.post("/api/getMovies").build());

        assertUnauthorized(result);
    }

    @Test
    void authPathIsPublic() {
        Result result = run(MockServerHttpRequest.post("/api/auth/login").build());

        assertNotNull(result.forwarded(), "/api/auth/** phải public");
        assertEquals(INTERNAL_TOKEN,
                result.forwarded().getRequest().getHeaders().getFirst(GatewayJwtFilter.INTERNAL_TOKEN_HEADER));
    }

    @Test
    void customerCanHoldSeat() {
        Result result = run(MockServerHttpRequest.post("/api/holdShowtimeSeat")
                .header("Authorization", "Bearer " + accessToken("CUSTOMER"))
                .build());

        assertNotNull(result.forwarded(), "Giữ ghế là thao tác của CUSTOMER");
    }

    @Test
    void missingJwtSecretFailsFast() {
        GatewaySecurityProperties properties =
                new GatewaySecurityProperties(null, ISSUER, INTERNAL_TOKEN);

        assertThrows(IllegalStateException.class, properties::afterPropertiesSet);
    }

    @Test
    void shortJwtSecretFailsFast() {
        GatewaySecurityProperties properties =
                new GatewaySecurityProperties("short", ISSUER, INTERNAL_TOKEN);

        assertThrows(IllegalStateException.class, properties::afterPropertiesSet);
    }

    @Test
    void missingInternalTokenFailsFast() {
        GatewaySecurityProperties properties =
                new GatewaySecurityProperties(SECRET, ISSUER, " ");

        assertThrows(IllegalStateException.class, properties::afterPropertiesSet);
    }

    @Test
    void shortInternalTokenFailsFast() {
        GatewaySecurityProperties properties =
                new GatewaySecurityProperties(SECRET, ISSUER, "too-short");

        assertThrows(IllegalStateException.class, properties::afterPropertiesSet);
    }

    private static MockServerHttpRequest.BaseBuilder<?> rawRequest(HttpMethod method, String rawPath) {
        return MockServerHttpRequest.method(method, URI.create("http://localhost" + rawPath));
    }

    private Result run(MockServerHttpRequest request) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        filter.filter(exchange, captured -> {
            forwarded.set(captured);
            return Mono.empty();
        }).block();
        return new Result(exchange, forwarded.get());
    }

    private static void assertUnauthorized(Result result) {
        assertNull(result.forwarded(), "Request không hợp lệ không được chuyển tiếp");
        assertEquals(HttpStatus.UNAUTHORIZED, result.exchange().getResponse().getStatusCode());
        assertTrue(result.exchange().getResponse().getBodyAsString().block().contains("đăng nhập"));
    }

    private static void assertForbidden(Result result) {
        assertNull(result.forwarded(), "Request sai quyền không được chuyển tiếp");
        assertEquals(HttpStatus.FORBIDDEN, result.exchange().getResponse().getStatusCode());
        String body = result.exchange().getResponse().getBodyAsString().block();
        assertTrue(body.contains("không có quyền"));
        assertFalse(body.contains("createMovie"), "Không lộ chi tiết rule");
    }

    private static void assertNotFound(Result result) {
        assertNull(result.forwarded(), "Path không chuẩn không được chuyển tiếp");
        assertEquals(HttpStatus.NOT_FOUND, result.exchange().getResponse().getStatusCode());
        assertTrue(result.exchange().getResponse().getBodyAsString().block().contains("Không tìm thấy"));
    }

    private static String accessToken(String role) {
        return token("access", role, Instant.now().plusSeconds(3600));
    }

    private static String token(String type, String role, Instant expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("42")
                .claim("role", role)
                .claim("type", type)
                .issuer(ISSUER)
                .issuedAt(Date.from(now.minusSeconds(60)))
                .expiration(Date.from(expiration))
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    private record Result(MockServerWebExchange exchange, ServerWebExchange forwarded) {
    }
}
