/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.exception;
/**
 *
 * @author quanghieu
 */
/** Base cho 401 UNAUTHORIZED (sai thông tin đăng nhập, token sai/hết hạn, ...). */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}