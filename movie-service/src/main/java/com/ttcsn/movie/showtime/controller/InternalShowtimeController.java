/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.controller;

import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.movie.showtime.repository.ShowtimeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/internal/showtimes")
@RequiredArgsConstructor
public class InternalShowtimeController {

    private final ShowtimeRepository showtimeRepository;

    @GetMapping("/{showtimeId}/exists")
    public boolean exists(@PathVariable Long showtimeId) {
        boolean exists = showtimeRepository.existsById(showtimeId);
        if (!exists) {
            throw new NotFoundException("Không tìm thấy suất chiếu với showtimeId = " + showtimeId);
        }
        return true;
    }
}