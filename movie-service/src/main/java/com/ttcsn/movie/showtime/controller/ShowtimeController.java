/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.controller;

import org.springframework.web.bind.annotation.*;
import com.ttcsn.movie.showtime.dto.request.ShowtimeRequest;
import com.ttcsn.movie.showtime.dto.response.ShowtimeResponse;
import com.ttcsn.movie.showtime.service.ShowtimeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.ttcsn.movie.showtime.controller.ShowtimeController;
import com.ttcsn.movie.showtime.dto.request.ShowtimeRequest;
import com.ttcsn.movie.showtime.dto.response.ShowtimeResponse;
import com.ttcsn.movie.showtime.service.ShowtimeService;

import java.util.List;

/**
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @GetMapping("/getShowtimes")
    public ResponseEntity<List<ShowtimeResponse>> getAllShowtimes(
            @RequestParam(required = false) Long movieId) {
        if (movieId != null) {
            return ResponseEntity.ok(showtimeService.getShowtimesByMovieId(movieId));
        }
        return ResponseEntity.ok(showtimeService.getAllShowtimes());
    }

    @GetMapping("/getShowtimeById")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(@RequestParam Long id) {
        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    /** Sơ đồ ghế theo suất chiếu: id + trạng thái (AVAILABLE/HELD/BOOKED) */
    @GetMapping("/getShowtimeSeats")
    public ResponseEntity<List<com.ttcsn.movie.showtime.dto.response.ShowtimeSeatView>> getShowtimeSeats(
            @RequestParam Long showtimeId) {
        return ResponseEntity.ok(showtimeService.getShowtimeSeats(showtimeId));
    }

    /** Giữ chỗ tạm 10 phút khi khách bấm chọn ghế (mô hình showtime_seats phát sinh theo tương tác). */
    @PostMapping("/holdShowtimeSeat")
    public ResponseEntity<com.ttcsn.movie.showtime.dto.response.ShowtimeSeatView> holdShowtimeSeat(
            @Valid @RequestBody com.ttcsn.movie.showtime.dto.request.ShowtimeSeatInteractionRequest request) {
        return ResponseEntity.ok(showtimeService.holdShowtimeSeat(
                request.showtimeId(), request.seatId(), request.holdBy()));
    }

    /** Bỏ giữ chỗ khi khách bỏ chọn ghế (chỉ người giữ hoặc giữ đã hết hạn). */
    @PostMapping("/releaseShowtimeSeat")
    public ResponseEntity<com.ttcsn.movie.showtime.dto.response.ShowtimeSeatView> releaseShowtimeSeat(
            @Valid @RequestBody com.ttcsn.movie.showtime.dto.request.ShowtimeSeatInteractionRequest request) {
        return ResponseEntity.ok(showtimeService.releaseShowtimeSeat(
                request.showtimeId(), request.seatId(), request.holdBy()));
    }

    @PostMapping("/createShowtime")
    public ResponseEntity<ShowtimeResponse> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        ShowtimeResponse response = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/updateShowtime")
    public ResponseEntity<ShowtimeResponse> updateShowtime(
            @RequestParam Long id,
            @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.ok(showtimeService.updateShowtime(id, request));
    }

    @DeleteMapping("/deleteShowtime")
    public ResponseEntity<Void> deleteShowtime(@RequestParam Long id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }
}