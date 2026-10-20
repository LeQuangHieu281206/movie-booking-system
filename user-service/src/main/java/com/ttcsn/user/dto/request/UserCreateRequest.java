/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.ttcsn.user.dto.request;

import com.ttcsn.user.entity.enums.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 *
 * @author quanghieu
 */
public record UserCreateRequest(
        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password,

        String phoneNumber,

        @NotNull(message = "Vai trò không được để trống")
        UserRole role
) {
}