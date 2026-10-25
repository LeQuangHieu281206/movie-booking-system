package com.ttcsn.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request đăng ký tài khoản. Không có field role — role luôn do server gán CUSTOMER.
 *
 * @param phoneNumber optional, không bắt buộc
 *
 * @author quanghieu
 */
public record RegisterRequest(
        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
        String password,

        String phoneNumber
) {
}
