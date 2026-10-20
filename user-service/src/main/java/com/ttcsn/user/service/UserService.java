/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.user.service;

import com.ttcsn.user.dto.request.UserCreateRequest;
import com.ttcsn.user.dto.request.UserUpdateRequest;
import com.ttcsn.user.dto.response.UserResponse;

import java.util.List;
/**
 *
 * @author quanghieu
 */
public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long id);
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
}