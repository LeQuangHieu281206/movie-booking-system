package com.ttcsn.booking.controller;

import org.springframework.web.bind.annotation.*;
import com.ttcsn.booking.dto.request.BookingCreateRequest;
import com.ttcsn.booking.dto.request.BookingUpdateRequest;
import com.ttcsn.booking.dto.response.BookingResponse;
import com.ttcsn.booking.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.controller.BookingController;
import com.ttcsn.booking.dto.request.BookingCreateRequest;
import com.ttcsn.booking.dto.response.BookingResponse;
import com.ttcsn.booking.service.BookingService;
import com.ttcsn.booking.dto.request.BookingUpdateRequest;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @GetMapping("/getBookings")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/getBookingById")
    public ResponseEntity<BookingResponse> getBookingById(@RequestParam Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/getBookingsByUserId")
    public ResponseEntity<List<BookingResponse>> getBookingsByUserId(@RequestParam Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    @GetMapping("/getBookingsByShowtimeId")
    public ResponseEntity<List<BookingResponse>> getBookingsByShowtimeId(@RequestParam Long showtimeId) {
        return ResponseEntity.ok(bookingService.getBookingsByShowtimeId(showtimeId));
    }

    @PostMapping("/createBooking")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingCreateRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @PutMapping("/updateBooking")
    public ResponseEntity<BookingResponse> updateBooking(@RequestParam Long id, @Valid @RequestBody BookingUpdateRequest request) {
        return ResponseEntity.ok(bookingService.updateBooking(id, request));
    }

    @DeleteMapping("/deleteBooking")
    public ResponseEntity<String> deleteBooking(@RequestParam Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.ok("Booking deleted successfully");
    }
}