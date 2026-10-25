package com.ttcsn.user.controller;

import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RegisterRequest;
import com.ttcsn.user.dto.response.AuthResponse;
import com.ttcsn.user.dto.response.UserSummary;
import com.ttcsn.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth công khai (permitAll trong SecurityConfig): register + login.
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Đăng ký tài khoản (role luôn CUSTOMER, mật khẩu băm BCrypt). Trả 201 + UserSummary. */
    @PostMapping("/register")
    public ResponseEntity<UserSummary> register(@Valid @RequestBody RegisterRequest request) {
        UserSummary user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /** Đăng nhập: sai thông tin -> 401 với thông báo chung; đúng -> 200 + AuthResponse. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
