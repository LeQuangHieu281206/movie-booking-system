package com.ttcsn.movie.showtime.controller;

import com.ttcsn.common.dto.SeatIdsRequest;
import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.movie.showtime.entity.ShowtimeSeat;
import com.ttcsn.movie.showtime.entity.enums.SeatStatus;
import com.ttcsn.movie.showtime.repository.ShowtimeSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal API — booking-service gọi sang để giữ/trả/chốt ghế.
 * KHÔNG expose qua gateway bên ngoài. (Step 5 sẽ thêm lock optimistic + TTL Redis)
 */
@RestController
@RequestMapping("/internal/showtime-seats")
@RequiredArgsConstructor
public class InternalShowtimeSeatController {

    private final ShowtimeSeatRepository showtimeSeatRepository;

    /** Đặt trạng thái ghế về BOOKED khi booking thành công. */
    @PostMapping("/hold")
    @Transactional
    public SeatHoldResult hold(@RequestBody SeatIdsRequest request) {
        List<Long> unavailable = request.showtimeSeatIds().stream()
                .filter(this::isSeatUnavailable)
                .toList();
        if (!unavailable.isEmpty()) {
            return new SeatHoldResult(false, unavailable, "Một hoặc nhiều ghế đã được đặt / đang giữ");
        }
        setSeatsStatus(request.showtimeSeatIds(), SeatStatus.BOOKED);
        return SeatHoldResult.ok();
    }

    /** Bù trừ (compensation) khi booking fail: trả ghế về AVAILABLE. */
    @PostMapping("/release")
    @Transactional
    public SeatHoldResult release(@RequestBody SeatIdsRequest request) {
        setSeatsStatus(request.showtimeSeatIds(), SeatStatus.AVAILABLE);
        return SeatHoldResult.ok();
    }

    /** Xác nhận sau khi payment completed (chốt thêm — hiện BOOKED đã là trạng thái chốt). */
    @PostMapping("/confirm")
    @Transactional
    public SeatHoldResult confirm(@RequestBody SeatIdsRequest request) {
        return SeatHoldResult.ok();
    }

    private boolean isSeatUnavailable(Long showtimeSeatId) {
        return showtimeSeatRepository.findById(showtimeSeatId)
                .map(ss -> ss.getStatus() != SeatStatus.AVAILABLE)
                .orElse(true);
    }

    private void setSeatsStatus(List<Long> showtimeSeatIds, SeatStatus status) {
        List<ShowtimeSeat> seats = showtimeSeatRepository.findAllById(showtimeSeatIds);
        seats.forEach(ss -> ss.setStatus(status));
        showtimeSeatRepository.saveAll(seats);
    }
}
