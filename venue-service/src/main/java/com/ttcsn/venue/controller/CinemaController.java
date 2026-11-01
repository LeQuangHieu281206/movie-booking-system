/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.venue.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ttcsn.venue.dto.request.CinemaRequest;
import com.ttcsn.venue.dto.response.CinemaResponse;
import com.ttcsn.venue.service.CinemaService;

import java.util.List;
/**
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CinemaController {

    private final CinemaService cinemaService;

    @GetMapping("/getCinemas")
    public ResponseEntity<List<CinemaResponse>> getAllCinemas() {
        return ResponseEntity.ok(cinemaService.getAllCinemas());
    }

    @GetMapping("/getCinemaById")
    public ResponseEntity<CinemaResponse> getCinemaById(@RequestParam Long id) {
        return ResponseEntity.ok(cinemaService.getCinemaById(id));
    }

    @GetMapping("/getCities")
    public ResponseEntity<List<String>> getAllCities() {
        return ResponseEntity.ok(cinemaService.getAllCities());
    }

    @GetMapping("/getCinemasByCity")
    public ResponseEntity<List<CinemaResponse>> getCinemasByCity(@RequestParam String city) {
        return ResponseEntity.ok(cinemaService.getCinemasByCity(city));
    }

    @PostMapping("/createCinema")
    public ResponseEntity<CinemaResponse> createCinema(@Valid @RequestBody CinemaRequest request) {
        CinemaResponse response = cinemaService.createCinema(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/updateCinema")
    public ResponseEntity<CinemaResponse> updateCinema(
            @RequestParam Long id,
            @Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.ok(cinemaService.updateCinema(id, request));
    }

    @DeleteMapping("/deleteCinema")
    public ResponseEntity<Void> deleteCinema(@RequestParam Long id) {
        cinemaService.deleteCinema(id);
        return ResponseEntity.noContent().build();
    }
}