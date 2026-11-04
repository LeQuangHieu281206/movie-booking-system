package com.ttcsn.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cập nhật hồ sơ của chính người dùng đang đăng nhập.
 * Không cho đổi email, role hay mật khẩu qua endpoint này.
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
        String fullName,
        @Size(max = 12, message = "Số điện thoại tối đa 12 ký tự")
        @Pattern(regexp = "^(|\\d{8,11}|\\+84\\d{9})$", message = "Số điện thoại phải gồm 8-11 chữ số hoặc +84 theo sau 9 chữ số")
        String phoneNumber
) {
}
