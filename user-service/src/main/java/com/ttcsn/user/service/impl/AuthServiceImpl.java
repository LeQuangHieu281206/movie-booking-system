package com.ttcsn.user.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ttcsn.user.dto.request.GoogleLoginRequest;
import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RegisterRequest;
import com.ttcsn.user.dto.response.AuthResponse;
import com.ttcsn.user.dto.response.UserSummary;
import com.ttcsn.user.entity.User;
import com.ttcsn.user.entity.enums.UserRole;
import com.ttcsn.user.exception.EmailAlreadyExistsException;
import com.ttcsn.user.exception.InvalidCredentialsException;
import com.ttcsn.user.exception.InvalidTokenException;
import com.ttcsn.user.repository.UserRepository;
import com.ttcsn.user.security.JwtService;
import com.ttcsn.user.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @author quanghieu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String GENERIC_LOGIN_ERROR = "Email hoặc mật khẩu không đúng";
    private static final String TOKEN_REJECTED = "Refresh token không hợp lệ hoặc đã hết hạn";
    private static final String GOOGLE_TOKEN_REJECTED = "Google ID Token không hợp lệ hoặc đã hết hạn";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String AUTH_PROVIDER_LOCAL = "LOCAL";
    private static final String AUTH_PROVIDER_GOOGLE = "GOOGLE";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Override
    @Transactional
    public UserSummary register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .authProvider(AUTH_PROVIDER_LOCAL)
                .phoneNumber(request.phoneNumber())
                .role(UserRole.CUSTOMER)
                .build();

        User saved = userRepository.save(user);
        return toSummary(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException(GENERIC_LOGIN_ERROR));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(GENERIC_LOGIN_ERROR);
        }

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserProfile profile = verifyGoogleIdToken(request.idToken());

        User user = findExistingUserForGoogle(profile)
                .map(existing -> syncGoogleIdentity(existing, profile))
                .orElseGet(() -> registerGoogleUser(profile));

        return issueTokens(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Claims claims = jwtService.parseAndVerify(refreshToken);
        jwtService.requireTokenType(claims, JwtService.CLAIM_TYPE_REFRESH);

        // Đọc lại user từ DB để dùng role/email hiện hành: token cũ của user bị
        // đổi quyền hay xóa tài khoản không thể cấp token mới với quyền cũ.
        Long userId = jwtService.extractUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException(TOKEN_REJECTED));

        // Rotation: mỗi lần refresh cấp cả access lẫn refresh token mới,
        // client phải lưu lại cặp mới (refresh token cũ vẫn còn hạn đến khi hết TTL).
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresIn = jwtService.accessExpirationMs() / 1000;
        return new AuthResponse(accessToken, refreshToken, TOKEN_TYPE, expiresIn, toSummary(user));
    }

    private Optional<User> findExistingUserForGoogle(GoogleUserProfile profile) {
        if (profile.googleId() != null && !profile.googleId().isBlank()) {
            Optional<User> byGoogleId = userRepository.findByGoogleId(profile.googleId());
            if (byGoogleId.isPresent()) {
                return byGoogleId;
            }
        }
        Optional<User> byEmailIgnoreCase = userRepository.findByEmailIgnoreCase(profile.email());
        if (byEmailIgnoreCase.isPresent()) {
            return byEmailIgnoreCase;
        }
        return userRepository.findByEmail(profile.email());
    }

    private User syncGoogleIdentity(User existing, GoogleUserProfile profile) {
        boolean updated = false;
        if (existing.getGoogleId() == null && profile.googleId() != null && !profile.googleId().isBlank()) {
            existing.setGoogleId(profile.googleId());
            updated = true;
        }
        if (existing.getAuthProvider() == null || existing.getAuthProvider().isBlank()) {
            existing.setAuthProvider(existing.getPasswordHash() == null ? AUTH_PROVIDER_GOOGLE : AUTH_PROVIDER_LOCAL);
            updated = true;
        }
        return updated ? userRepository.save(existing) : existing;
    }

    private User registerGoogleUser(GoogleUserProfile profile) {
        User newUser = User.builder()
                .fullName(profile.fullName())
                .email(profile.email())
                .passwordHash(null)
                .authProvider(AUTH_PROVIDER_GOOGLE)
                .googleId(profile.googleId())
                .role(UserRole.CUSTOMER)
                .build();
        return userRepository.save(newUser);
    }

    private GoogleUserProfile verifyGoogleIdToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException(GOOGLE_TOKEN_REJECTED);
        }
        String token = rawToken.trim();

        if (!isPlaceholderClientId(googleClientId)) {
            try {
                GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance()
                )
                        .setAudience(Collections.singletonList(googleClientId.trim()))
                        .build();

                GoogleIdToken idToken = verifier.verify(token);
                if (idToken == null) {
                    log.warn("Xác thực Google ID Token thất bại với GOOGLE_CLIENT_ID đã cấu hình.");
                    throw new InvalidTokenException(GOOGLE_TOKEN_REJECTED);
                }

                GoogleIdToken.Payload payload = idToken.getPayload();
                if (Boolean.FALSE.equals(payload.getEmailVerified())) {
                    log.warn("Email Google chưa được xác minh cho subject={}", payload.getSubject());
                    throw new InvalidTokenException("Tài khoản Google chưa xác minh email");
                }
                return extractProfile(
                        payload.getEmail(),
                        (String) payload.get("name"),
                        payload.getSubject()
                );
            } catch (InvalidTokenException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("Lỗi khi xác thực Google ID Token: {}", ex.getMessage());
                throw new InvalidTokenException(GOOGLE_TOKEN_REJECTED);
            }
        }

        log.warn("GOOGLE_CLIENT_ID đang để trống hoặc giá trị placeholder ('{}'). "
                + "Sử dụng chế độ giải mã payload an toàn phục vụ môi trường dev/local.", googleClientId);
        return decodePayloadSafely(token);
    }

    private boolean isPlaceholderClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return true;
        }
        String normalized = clientId.trim().toLowerCase();
        return normalized.startsWith("your-")
                || normalized.contains("placeholder")
                || normalized.equals("your-google-client-id.apps.googleusercontent.com");
    }

    private GoogleUserProfile decodePayloadSafely(String token) {
        try {
            GoogleIdToken parsed = GoogleIdToken.parse(GsonFactory.getDefaultInstance(), token);
            GoogleIdToken.Payload payload = parsed.getPayload();
            if (payload.getExpirationTimeSeconds() != null
                    && payload.getExpirationTimeSeconds() * 1000L < System.currentTimeMillis()) {
                throw new InvalidTokenException(GOOGLE_TOKEN_REJECTED);
            }
            if (Boolean.FALSE.equals(payload.getEmailVerified())) {
                throw new InvalidTokenException("Tài khoản Google chưa xác minh email");
            }
            return extractProfile(
                    payload.getEmail(),
                    (String) payload.get("name"),
                    payload.getSubject()
            );
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ignored) {
            // Fallback cho JWT 3 phần hoặc JSON base64url khi test local
        }

        try {
            String[] parts = token.split("\\.");
            String payloadSegment = parts.length >= 2 ? parts[1] : parts[0];
            byte[] decoded = Base64.getUrlDecoder().decode(payloadSegment);
            Map<String, Object> claims = OBJECT_MAPPER.readValue(
                    new String(decoded, StandardCharsets.UTF_8),
                    new TypeReference<>() {}
            );
            Object expObj = claims.get("exp");
            if (expObj instanceof Number expNum && expNum.longValue() * 1000L < System.currentTimeMillis()) {
                throw new InvalidTokenException(GOOGLE_TOKEN_REJECTED);
            }
            String email = claims.get("email") != null ? String.valueOf(claims.get("email")) : null;
            String name = claims.get("name") != null ? String.valueOf(claims.get("name")) : null;
            String sub = claims.get("sub") != null ? String.valueOf(claims.get("sub")) : null;
            return extractProfile(email, name, sub);
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Không thể giải mã Google ID Token payload: {}", ex.getMessage());
            throw new InvalidTokenException(GOOGLE_TOKEN_REJECTED);
        }
    }

    private GoogleUserProfile extractProfile(String email, String name, String subject) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new InvalidTokenException(GOOGLE_TOKEN_REJECTED);
        }
        String normalizedEmail = email.trim().toLowerCase();
        String resolvedName = (name != null && !name.isBlank())
                ? name.trim()
                : normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
        if (resolvedName.length() < 2) {
            resolvedName = resolvedName + " User";
        }
        String googleId = (subject != null && !subject.isBlank())
                ? subject.trim()
                : "google:" + normalizedEmail;
        return new GoogleUserProfile(normalizedEmail, resolvedName, googleId);
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(user.getUserId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    private record GoogleUserProfile(String email, String fullName, String googleId) {
    }
}
