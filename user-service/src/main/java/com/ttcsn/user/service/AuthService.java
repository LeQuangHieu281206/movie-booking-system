package com.ttcsn.user.service;

import com.ttcsn.user.dto.request.GoogleLoginRequest;
import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RegisterRequest;
import com.ttcsn.user.dto.response.AuthResponse;
import com.ttcsn.user.dto.response.UserSummary;

/**
 * Dịch vụ auth: register + login + googleLogin + refresh (stateless JWT, cặp access/refresh token).
 *
 * @author quanghieu
 */
public interface AuthService {
    UserSummary register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse googleLogin(GoogleLoginRequest request);

    /**
     * Đổi refresh token hợp lệ (type = "refresh") lấy cặp token mới.
     * Token sai/hết hạn/sai loại hoặc user đã bị xóa → InvalidTokenException (401).
     */
    AuthResponse refresh(String refreshToken);
}
