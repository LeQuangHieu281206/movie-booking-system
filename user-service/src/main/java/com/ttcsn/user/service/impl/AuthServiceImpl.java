package com.ttcsn.user.service.impl;

import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RegisterRequest;
import com.ttcsn.user.dto.response.AuthResponse;
import com.ttcsn.user.dto.response.UserSummary;
import com.ttcsn.user.entity.User;
import com.ttcsn.user.entity.enums.UserRole;
import com.ttcsn.user.exception.EmailAlreadyExistsException;
import com.ttcsn.user.exception.InvalidCredentialsException;
import com.ttcsn.user.repository.UserRepository;
import com.ttcsn.user.security.JwtService;
import com.ttcsn.user.service.AuthService;
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
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(GENERIC_LOGIN_ERROR);
        }

        String accessToken = jwtService.generateAccessToken(user);
        long expiresIn = jwtService.accessExpirationMs() / 1000;
        return new AuthResponse(accessToken, TOKEN_TYPE, expiresIn, toSummary(user));
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(user.getUserId(), user.getEmail(), user.getFullName(), user.getRole());
    }
}
