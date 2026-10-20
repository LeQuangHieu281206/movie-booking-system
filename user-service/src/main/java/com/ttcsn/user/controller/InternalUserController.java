package com.ttcsn.user.controller;

import com.ttcsn.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal API — booking-service kiểm tra user tồn tại. */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}/exists")
    public boolean exists(@PathVariable Long userId) {
        return userRepository.existsById(userId);
    }
}
