package com.ttcsn.user.dto.response;

import com.ttcsn.user.entity.enums.UserRole;

/**
 * Dữ liệu user gọn cho AuthResponse — đúng spec: {"id": 1, "email": ..., "fullName": ..., "role": ...}.
 * Không bao giờ chứa passwordHash.
 *
 * @author quanghieu
 */
public record UserSummary(
        Long id,
        String email,
        String fullName,
        UserRole role
) {
}
