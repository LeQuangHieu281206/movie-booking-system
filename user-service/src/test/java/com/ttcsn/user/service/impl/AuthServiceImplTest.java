package com.ttcsn.user.service.impl;

import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RefreshTokenRequest;
import com.ttcsn.user.dto.request.RegisterRequest;
import com.ttcsn.user.dto.response.AuthResponse;
import com.ttcsn.user.entity.User;
import com.ttcsn.user.entity.enums.UserRole;
import com.ttcsn.user.exception.InvalidCredentialsException;
import com.ttcsn.user.exception.InvalidTokenException;
import com.ttcsn.user.repository.UserRepository;
import com.ttcsn.user.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test AuthService thuần Mockito, không cần Spring context/DB thật.
 * Phần token-level (sai chữ ký, hết hạn, sai type) đã phủ ở JwtServiceTest;
 * ở đây mock JwtService để test logic ủy quyền: nạp lại user từ DB, role hiện tại, 401 chung.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String GENERIC_TOKEN_ERROR = "Token khong hop le hoac da het han";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthServiceImpl authService;

    // ---------- login ----------

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        User user = user(12L, UserRole.CUSTOMER);
        when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("acc");
        when(jwtService.generateRefreshToken(user)).thenReturn("ref");
        when(jwtService.accessExpirationMs()).thenReturn(7_200_000L);

        AuthResponse res = authService.login(new LoginRequest("a@b.c", "pw"));

        assertEquals("acc", res.accessToken());
        assertNotNull(res.refreshToken(), "login phải trả refreshToken");
        assertEquals("Bearer", res.tokenType());
        assertEquals(7200L, res.expiresIn());
        assertEquals(12L, res.user().id());
    }

    @Test
    void loginWrongPasswordThrowsGenericError() {
        User user = user(12L, UserRole.CUSTOMER);
        when(userRepository.findByEmail("a@b.c")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("a@b.c", "wrong")));
        verify(jwtService, never()).generateAccessToken(any());
    }

    // ---------- register ----------

    @Test
    void registerCreatesCustomerAndReturnsTokens() {
        when(userRepository.existsByEmail("new@b.c")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(13L);
            return u;
        });
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("acc");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("ref");
        when(jwtService.accessExpirationMs()).thenReturn(7_200_000L);

        AuthResponse res = authService.register(
                new RegisterRequest("New User", "new@b.c", "pw", "0900000000"));

        assertEquals("acc", res.accessToken());
        assertNotNull(res.refreshToken(), "register phải trả refreshToken (đăng ký xong được cấp luôn)");
        assertEquals(UserRole.CUSTOMER, res.user().role(), "role đăng ký luôn CUSTOMER");
    }

    // ---------- refresh ----------

    @Test
    void refreshReturnsNewAccessTokenWithCurrentRoleFromDb() {
        Claims claims = mock(Claims.class);
        when(jwtService.parseAndVerify("ref-token")).thenReturn(claims);
        when(jwtService.requireTokenType(claims, JwtService.CLAIM_TYPE_REFRESH)).thenReturn(claims);
        when(jwtService.extractUserId(claims)).thenReturn(12L);
        // Token cấp lúc còn CUSTOMER nhưng DB đã nâng lên ADMIN -> token mới mang role hiện tại
        User current = user(12L, UserRole.ADMIN);
        when(userRepository.findById(12L)).thenReturn(Optional.of(current));
        when(jwtService.generateAccessToken(current)).thenReturn("acc-new");

        AuthResponse res = authService.refresh(new RefreshTokenRequest("ref-token"));

        assertEquals("acc-new", res.accessToken());
        assertNull(res.refreshToken(), "không xoay vòng: refresh trả refreshToken = null, client giữ token cũ");
        assertEquals(UserRole.ADMIN, res.user().role());
    }

    @Test
    void refreshRejectsAccessTokenAsRefreshToken() {
        Claims claims = mock(Claims.class);
        when(jwtService.parseAndVerify("access-token")).thenReturn(claims);
        when(jwtService.requireTokenType(claims, JwtService.CLAIM_TYPE_REFRESH))
                .thenThrow(new InvalidTokenException(GENERIC_TOKEN_ERROR));

        InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> authService.refresh(new RefreshTokenRequest("access-token")));
        assertEquals(GENERIC_TOKEN_ERROR, ex.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void refreshRejectsTamperedOrExpiredToken() {
        // hết hạn/sai chữ ký đều bị chặn ở parseAndVerify (chi tiết token-level ở JwtServiceTest)
        when(jwtService.parseAndVerify("bad-token"))
                .thenThrow(new InvalidTokenException(GENERIC_TOKEN_ERROR));

        InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> authService.refresh(new RefreshTokenRequest("bad-token")));
        assertEquals(GENERIC_TOKEN_ERROR, ex.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void refreshRejectsDeletedUserWithGenericMessage() {
        Claims claims = mock(Claims.class);
        when(jwtService.parseAndVerify("ref-token")).thenReturn(claims);
        when(jwtService.requireTokenType(claims, JwtService.CLAIM_TYPE_REFRESH)).thenReturn(claims);
        when(jwtService.extractUserId(claims)).thenReturn(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> authService.refresh(new RefreshTokenRequest("ref-token")));
        // Thông báo chung — không tiết lộ user đã từng tồn tại hay chưa.
        assertEquals(GENERIC_TOKEN_ERROR, ex.getMessage());
        verify(jwtService, never()).generateAccessToken(any());
    }

    private static User user(Long id, UserRole role) {
        return User.builder()
                .userId(id)
                .fullName("User " + id)
                .email("user" + id + "@test.local")
                .passwordHash("hash")
                .role(role)
                .build();
    }
}
