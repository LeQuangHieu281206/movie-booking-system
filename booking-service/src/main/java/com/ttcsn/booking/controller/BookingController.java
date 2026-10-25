package com.ttcsn.booking.controller;

import com.ttcsn.booking.dto.request.BookingCreateRequest;
import com.ttcsn.booking.dto.request.BookingUpdateRequest;
import com.ttcsn.booking.dto.response.BookingResponse;
import com.ttcsn.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @GetMapping("/getBookings")
    public ResponseEntity<List<BookingResponse>> getAllBookings(
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(bookingService.getAllBookings(role));
    }

    @GetMapping("/getBookingById")
    public ResponseEntity<BookingResponse> getBookingById(
            @RequestParam Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(bookingService.getBookingById(id, userId, role));
    }

    @GetMapping("/getBookingsByUserId")
    public ResponseEntity<List<BookingResponse>> getBookingsByUserId(
            @RequestParam(required = false) Long userId,
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId, currentUserId, role));
    }

    @GetMapping("/getBookingsByShowtimeId")
    public ResponseEntity<List<BookingResponse>> getBookingsByShowtimeId(
            @RequestParam Long showtimeId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(bookingService.getBookingsByShowtimeId(showtimeId, role));
    }

    @PostMapping("/createBooking")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(bookingService.createBooking(request, userId));
    }

    @PutMapping("/updateBooking")
    public ResponseEntity<BookingResponse> updateBooking(
            @RequestParam Long id,
            @Valid @RequestBody BookingUpdateRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(bookingService.updateBooking(id, request, userId, role));
    }

    @DeleteMapping("/deleteBooking")
    public ResponseEntity<String> deleteBooking(
            @RequestParam Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        bookingService.deleteBooking(id, userId, role);
        return ResponseEntity.ok("Booking deleted successfully");
    }
}
