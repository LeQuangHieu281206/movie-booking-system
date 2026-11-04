/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.ttcsn.user.dto.request;

import com.ttcsn.user.entity.enums.UserRole;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 *
 * @author quanghieu
 */
public record UserUpdateRequest(
        String fullName,
        String password,
        @Size(max = 12, message = "Số điện thoại tối đa 12 ký tự")
        @Pattern(regexp = "^(|\\d{8,11}|\\+84\\d{9})$", message = "Số điện thoại phải gồm 8-11 chữ số hoặc +84 theo sau 9 chữ số")
        String phoneNumber,
        UserRole role
) {
}