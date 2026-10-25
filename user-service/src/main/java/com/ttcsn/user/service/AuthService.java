package com.ttcsn.user.service;

import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RefreshTokenRequest;
import com.ttcsn.user.dto.request.RegisterRequest;
import com.ttcsn.user.dto.response.AuthResponse;

/**
 * Dịch vụ auth: register + login + refresh (stateless JWT, không lưu token vào DB).
 *
 * @author quanghieu
 */
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
}
