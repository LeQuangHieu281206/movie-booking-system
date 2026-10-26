/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.controller;

import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.common.dto.SeatIdsRequest;
import com.ttcsn.movie.showtime.service.ShowtimeService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/internal/showtime-seats")
@RequiredArgsConstructor
public class InternalShowtimeSeatController {

    private final ShowtimeService showtimeService;

    @PostMapping("/hold")
    public SeatHoldResult hold(@RequestBody SeatIdsRequest request) {
        return showtimeService.bookSeats(request.showtimeId(), request.seatIds(), request.holdBy());
    }

    @PostMapping("/release")
    public SeatHoldResult release(@RequestBody SeatIdsRequest request) {
        showtimeService.releaseSeatsInternal(request.showtimeId(), request.seatIds());
        return SeatHoldResult.ok();
    }
}