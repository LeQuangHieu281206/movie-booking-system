package com.ttcsn.user.service.impl;

import com.ttcsn.user.config.JwtProperties;
import com.ttcsn.user.dto.request.GoogleLoginRequest;
import com.ttcsn.user.dto.response.AuthResponse;
import com.ttcsn.user.entity.User;
import com.ttcsn.user.entity.enums.UserRole;
import com.ttcsn.user.exception.InvalidTokenException;
import com.ttcsn.user.repository.UserRepository;
import com.ttcsn.user.security.JwtService;
import com.ttcsn.user.security.impl.JwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho luồng đăng nhập bằng Google OAuth2 ID Token trong AuthServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SECRET = "unit-test-secret-with-at-least-32-characters-ok!";
    private static final String ISSUER = "mtbs-user-service";

    @Mock
    private UserRepository userRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtService jwtService = new JwtServiceImpl(
                new JwtProperties(SECRET, 7_200_000L, 604_800_000L, ISSUER)
        );
        authService = new AuthServiceImpl(
                userRepository,
                new BCryptPasswordEncoder(),
                jwtService
        );
        ReflectionTestUtils.setField(
                authService,
                "googleClientId",
                "your-google-client-id.apps.googleusercontent.com"
        );
    }

    @Test
    void googleLoginAutoRegistersNewCustomerWhenEmailNotFound() {
        String idToken = buildMockIdToken("newuser@gmail.com", "Nguyen Van Google", "google-sub-123");

        when(userRepository.findByGoogleId("google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("newuser@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(101L);
            return u;
        });

        AuthResponse response = authService.googleLogin(new GoogleLoginRequest(idToken));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("GOOGLE", saved.getAuthProvider());
        assertEquals("google-sub-123", saved.getGoogleId());
        assertEquals("newuser@gmail.com", saved.getEmail());
        assertEquals("Nguyen Van Google", saved.getFullName());
        assertEquals(UserRole.CUSTOMER, saved.getRole());
        assertNull(saved.getPasswordHash());

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(101L, response.user().id());
        assertEquals("newuser@gmail.com", response.user().email());
        assertEquals(UserRole.CUSTOMER, response.user().role());
    }

    @Test
    void googleLoginReusesExistingUserAndLinksGoogleId() {
        String idToken = buildMockIdToken("existing@gmail.com", "Existing User", "google-sub-999");

        User existing = User.builder()
                .userId(55L)
                .fullName("Existing User")
                .email("existing@gmail.com")
                .passwordHash("$2a$10$hash")
                .authProvider("LOCAL")
                .role(UserRole.CUSTOMER)
                .build();

        when(userRepository.findByGoogleId("google-sub-999")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("existing@gmail.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.googleLogin(new GoogleLoginRequest(idToken));

        assertEquals("google-sub-999", existing.getGoogleId());
        assertEquals(55L, response.user().id());
        assertNotNull(response.accessToken());
    }

    @Test
    void googleLoginRejectsMalformedToken() {
        assertThrows(
                InvalidTokenException.class,
                () -> authService.googleLogin(new GoogleLoginRequest("not-a-valid-jwt-token"))
        );
    }

    private static String buildMockIdToken(String email, String name, String sub) {
        long exp = (System.currentTimeMillis() / 1000L) + 3600L;
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = String.format(
                "{\"iss\":\"https://accounts.google.com\",\"sub\":\"%s\",\"email\":\"%s\",\"email_verified\":true,\"name\":\"%s\",\"exp\":%d}",
                sub, email, name, exp
        );
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }
}
