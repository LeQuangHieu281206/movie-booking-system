package com.ttcsn.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cập nhật hồ sơ của chính người dùng đang đăng nhập.
 * Không cho đổi email, role hay mật khẩu qua endpoint này.
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        String fullName,
        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phoneNumber
) {
}
