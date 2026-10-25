package com.ttcsn.user.dto.response;

/**
 * Response trả về sau khi login hoặc refresh thành công.
 *
 * @param accessToken  JWT truy cập các endpoint cần đăng nhập (TTL ngắn)
 * @param refreshToken JWT làm mới access token (TTL dài, chỉ gửi cho /api/auth/refresh)
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
