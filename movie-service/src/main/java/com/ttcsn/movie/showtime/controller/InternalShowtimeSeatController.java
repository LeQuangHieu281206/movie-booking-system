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
 * Internal API — booking-service gọi sang để chốt/trả ghế theo mô hình
 * showtime_seats phát sinh theo tương tác (seatId = ghế vật lý của venue).
 * KHÔNG expose qua gateway bên ngoài.
 */
@RestController
@RequestMapping("/internal/showtime-seats")
@RequiredArgsConstructor
public class InternalShowtimeSeatController {

    private final ShowtimeService showtimeService;

    /** Chốt ghế BOOKED khi booking tạo thành công (chèn bản ghi nếu ghế chưa có). */
    @PostMapping("/hold")
    public SeatHoldResult hold(@RequestBody SeatIdsRequest request) {
        return showtimeService.bookSeats(request.showtimeId(), request.seatIds(), request.holdBy());
    }

    /** Bù trừ (compensation) khi booking fail / hủy vé: trả ghế về AVAILABLE. */
    @PostMapping("/release")
    public SeatHoldResult release(@RequestBody SeatIdsRequest request) {
        showtimeService.releaseSeatsInternal(request.showtimeId(), request.seatIds());
        return SeatHoldResult.ok();
    }
}
