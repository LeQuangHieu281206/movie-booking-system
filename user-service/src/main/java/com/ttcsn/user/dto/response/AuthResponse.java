package com.ttcsn.user.dto.response;

/**
 * Response trả về sau khi login/register thành công.
 *
 * @param accessToken  JWT truy cập các endpoint cần đăng nhập
 * @param refreshToken JWT gia hạn (type = "refresh", TTL dài). Endpoint /api/auth/refresh
 *                     trả field này bằng null vì không xoay vòng refresh token.
 * @param tokenType    luôn "Bearer"
 * @param expiresIn    thời gian sống của access token tính bằng GIÂY
 * @param user         thông tin user vừa đăng nhập
 *
 * @author quanghieu
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
}
