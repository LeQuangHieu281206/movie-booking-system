package com.ttcsn.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body của POST /api/auth/refresh. Chỉ nhận refreshToken do client đã lưu
 * từ lúc login/register; userId và role luôn đọc lại từ token + DB, không tin body.
 *
 * @author quanghieu
 */
public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken không được để trống") String refreshToken
) {
}
