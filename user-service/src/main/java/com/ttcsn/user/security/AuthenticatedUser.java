package com.ttcsn.user.security;

/**
 * Principal đặt vào SecurityContext sau khi JWT hợp lệ.
 * Được tạo từ claims của access token, KHÔNG truy vấn DB (stateless).
 * Controller lấy user hiện tại qua {@code @AuthenticationPrincipal AuthenticatedUser user}.
 *
 * @param id   userId (từ claim sub)
 * @param email (từ claim email)
 * @param role  UserRole.name() (từ claim role, đã validate là giá trị enum hợp lệ)
 *
 * @author quanghieu
 */
public record AuthenticatedUser(Long id, String email, String role) {
}
