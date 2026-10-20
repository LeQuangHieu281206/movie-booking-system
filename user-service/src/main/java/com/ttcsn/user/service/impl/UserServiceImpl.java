/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.user.service.impl;

import com.ttcsn.user.dto.request.UserCreateRequest;
import com.ttcsn.user.dto.request.UserUpdateRequest;
import com.ttcsn.user.dto.response.UserResponse;
import com.ttcsn.user.entity.User;
import com.ttcsn.user.exception.EmailAlreadyExistsException;
import com.ttcsn.user.exception.UserNotFoundException;
import com.ttcsn.user.repository.UserRepository;
import com.ttcsn.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
/**
 *
 * @author quanghieu
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return convertToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(request.password())
                .phoneNumber(request.phoneNumber())
                .role(request.role())
                .build();

        User savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(request.password());
        }

        User updatedUser = userRepository.save(user);
        return convertToResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private UserResponse convertToResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole()
        );
    }
}