package com.ttcsn.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request làm mới access token: client gửi lại refresh token nhận được khi login.
 *
 * @author quanghieu
 */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken
) {
}
