package com.ttcsn.user.service;

import com.ttcsn.user.dto.request.LoginRequest;
import com.ttcsn.user.dto.request.RegisterRequest;
import com.ttcsn.user.dto.response.AuthResponse;
import com.ttcsn.user.dto.response.UserSummary;

/**
 * Dịch vụ auth: register + login (stateless JWT, chỉ access token).
 *
 * @author quanghieu
 */
public interface AuthService {
    UserSummary register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
