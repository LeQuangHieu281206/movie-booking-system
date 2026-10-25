package com.ttcsn.user.service.impl;

import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RefreshTokenRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author quanghieu
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String GENERIC_LOGIN_ERROR = "Email hoặc mật khẩu không đúng";
    /** Trùng thông báo chung của JwtServiceImpl — không tiết lộ token sai vì lý do gì. */
    private static final String GENERIC_TOKEN_ERROR = "Token khong hop le hoac da het han";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .role(UserRole.CUSTOMER)
                .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException(GENERIC_LOGIN_ERROR));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(GENERIC_LOGIN_ERROR);
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        // parseAndVerify/requireTokenType tự ném InvalidTokenException (401, thông báo chung)
        // khi sai chữ ký, hết hạn hoặc đây là access token (type sai).
        Claims claims = jwtService.requireTokenType(
                jwtService.parseAndVerify(request.refreshToken()),
                JwtService.CLAIM_TYPE_REFRESH);

        // Nạp lại user từ DB: token còn hạn nhưng user đã bị xóa thì vẫn từ chối,
        // và role trong token mới luôn lấy theo role hiện tại trong DB.
        Long userId = jwtService.extractUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException(GENERIC_TOKEN_ERROR));

        String accessToken = jwtService.generateAccessToken(user);
        long expiresIn = jwtService.accessExpirationMs() / 1000;
        // Không xoay vòng refresh token: client giữ nguyên refreshToken cũ cho lần refresh sau.
        return new AuthResponse(accessToken, null, TOKEN_TYPE, expiresIn, toSummary(user));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresIn = jwtService.accessExpirationMs() / 1000;
        return new AuthResponse(accessToken, refreshToken, TOKEN_TYPE, expiresIn, toSummary(user));
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(user.getUserId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
