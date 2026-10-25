package com.ttcsn.user.dto.response;

/**
 * Response trả về sau khi login thành công.
 *
 * @param accessToken JWT truy cập các endpoint cần đăng nhập
 * @param tokenType   luôn "Bearer"
 * @param expiresIn   thời gian sống của access token tính bằng GIÂY
 * @param user        thông tin user vừa đăng nhập
 *
 * @author quanghieu
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
}
