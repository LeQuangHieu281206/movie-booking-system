/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.venue.controller;

import org.springframework.web.bind.annotation.*;
import com.ttcsn.venue.dto.request.SeatRequest;
import com.ttcsn.venue.dto.response.SeatResponse;
import com.ttcsn.venue.service.SeatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ttcsn.venue.controller.SeatController;
import com.ttcsn.venue.dto.request.SeatRequest;
import com.ttcsn.venue.dto.response.SeatResponse;
import com.ttcsn.venue.service.SeatService;

import java.util.List;

/**
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/getSeatsByRoomId")
    public ResponseEntity<List<SeatResponse>> getSeatsByRoom(@RequestParam Long roomId) {
        return ResponseEntity.ok(seatService.getSeatsByRoomId(roomId));
    }

    @GetMapping("/getSeatById")
    public ResponseEntity<SeatResponse> getSeatById(@RequestParam Long id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    @PostMapping("/createSeat")
    public ResponseEntity<SeatResponse> createSeat(@Valid @RequestBody SeatRequest request) {
        SeatResponse response = seatService.createSeat(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/updateSeat")
    public ResponseEntity<SeatResponse> updateSeat(
            @RequestParam Long id,
            @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }

    @DeleteMapping("/deleteSeat")
    public ResponseEntity<Void> deleteSeat(@RequestParam Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}