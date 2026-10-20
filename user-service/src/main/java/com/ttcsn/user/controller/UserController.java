package com.ttcsn.user.controller;

import org.springframework.web.bind.annotation.*;
import com.ttcsn.user.dto.request.UserCreateRequest;
import com.ttcsn.user.dto.request.UserUpdateRequest;
import com.ttcsn.user.dto.response.UserResponse;
import com.ttcsn.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.ttcsn.user.entity.User;
import com.ttcsn.user.controller.UserController;
import com.ttcsn.user.dto.request.UserCreateRequest;
import com.ttcsn.user.dto.response.UserResponse;
import com.ttcsn.user.service.UserService;
import com.ttcsn.user.dto.request.UserUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/getUsers")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/getUserById")
    public ResponseEntity<UserResponse> getUserById(@RequestParam Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/createUser")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PutMapping("/updateUser")
    public ResponseEntity<UserResponse> updateUser(@RequestParam Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/deleteUser")
    public ResponseEntity<String> deleteUser(@RequestParam Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }
}