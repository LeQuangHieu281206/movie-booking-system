package com.ttcsn.booking.service;

import com.ttcsn.booking.client.ShowtimeClient;
import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.entity.BookingDetail;
import com.ttcsn.booking.repository.BookingDetailRepository;
import com.ttcsn.common.dto.SeatIdsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Trả ghế của một booking về AVAILABLE ở movie-service.
 * Dùng chung cho huỷ/hết hạn qua API và event thanh toán FAILED — một nơi duy nhất
 * quyết định cách bù trừ và cách nuốt lỗi Feign (không rollback trạng thái booking).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSeatReleaser {

    private final BookingDetailRepository bookingDetailRepository;
    private final ShowtimeClient showtimeClient;

    public void release(Booking booking) {
        List<Long> seatIds = bookingDetailRepository
                .findByBooking_BookingId(booking.getBookingId()).stream()
                .map(BookingDetail::getSeatId)
                .toList();
        if (seatIds.isEmpty()) {
            return;
        }
        try {
            showtimeClient.releaseSeats(new SeatIdsRequest(booking.getShowtimeId(), seatIds, null));
            log.info("[booking-service] Đã trả ghế về AVAILABLE cho booking {} (bù trừ)",
                    booking.getBookingId());
        } catch (RuntimeException ex) {
            log.error("[booking-service] KHÔNG trả được ghế về AVAILABLE cho booking {}: {}",
                    booking.getBookingId(), ex.getMessage());
        }
    }
}
