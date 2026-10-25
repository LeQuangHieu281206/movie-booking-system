package com.ttcsn.user.controller;

import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RefreshTokenRequest;
import com.ttcsn.user.dto.request.RegisterRequest;
import com.ttcsn.user.dto.response.AuthResponse;
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
 * Auth công khai (permitAll trong SecurityConfig): register + login + refresh.
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Đăng ký tài khoản (role luôn CUSTOMER, mật khẩu băm BCrypt), trả kèm bộ token đăng nhập luôn. Trả 201 + AuthResponse. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse auth = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(auth);
    }

    /** Đăng nhập: sai thông tin -> 401 với thông báo chung; đúng -> 200 + AuthResponse (accessToken + refreshToken). */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Đổi refreshToken lấy accessToken mới (role nạp lại từ DB).
     * Sai chữ ký/hết hạn/type sai/user đã xóa -> 401 thông báo chung.
     * Không xoay vòng refreshToken — client giữ nguyên token cũ.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
