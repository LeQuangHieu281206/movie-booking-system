package com.ttcsn.booking.controller;

import com.ttcsn.booking.service.BookingService;
import com.ttcsn.common.dto.BookingPaymentView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API cho payment-service xác thực booking trước khi publish event.
 * Không có route qua gateway; backend chỉ bind 127.0.0.1.
 */
@RestController
@RequestMapping("/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingService bookingService;

    @GetMapping("/{bookingId}")
    public BookingPaymentView getBooking(@PathVariable Long bookingId) {
        return bookingService.getPaymentView(bookingId);
    }
}
