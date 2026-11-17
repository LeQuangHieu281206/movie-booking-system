package com.ttcsn.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request đăng nhập bằng Google OAuth2 ID Token.
 *
 * @author quanghieu
 */
public record GoogleLoginRequest(
        @NotBlank(message = "Google ID Token không được để trống")
        String idToken
) {
}
