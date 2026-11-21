package com.ttcsn.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Đổi mật khẩu của chính người dùng đang đăng nhập.
 * Tài khoản tạo bằng Google chưa có mật khẩu nên không cần nhập mật khẩu hiện tại.
 */
public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8 đến 100 ký tự")
        String newPassword
) {
}
