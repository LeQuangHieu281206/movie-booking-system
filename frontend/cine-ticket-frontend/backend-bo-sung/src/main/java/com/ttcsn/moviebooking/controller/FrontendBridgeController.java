package com.ttcsn.moviebooking.controller;

import com.ttcsn.moviebooking.dto.response.BookingResponse;
import com.ttcsn.moviebooking.entity.Booking;
import com.ttcsn.moviebooking.entity.BookingDetail;
import com.ttcsn.moviebooking.entity.Showtime;
import com.ttcsn.moviebooking.entity.ShowtimeSeat;
import com.ttcsn.moviebooking.entity.User;
import com.ttcsn.moviebooking.entity.enums.BookingStatus;
import com.ttcsn.moviebooking.entity.enums.PaymentMethod;
import com.ttcsn.moviebooking.entity.enums.SeatStatus;
import com.ttcsn.moviebooking.entity.enums.SeatType;
import com.ttcsn.moviebooking.entity.enums.ShowtimeStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API adapter for the supplied movie-booking-system backend (Java 21).
 * Local coursework integration only: original backend has no authentication.
 * Does not replace or modify the original controllers.
 */
@RestController
@RequestMapping("/api/frontend")
@RequiredArgsConstructor
public class FrontendBridgeController {
    private final EntityManager em;

    public record ShowtimeSeatView(Long showtimeSeatId, Long seatId,
                                   String seatRow, Short seatNumber,
                                   SeatType seatType, SeatStatus status) {}

    public record CreateBookingInput(
            @NotNull @Positive Long userId,
            @NotNull @Positive Long showtimeId,
            @NotEmpty @Size(max = 8) List<@NotNull @Positive Long> showtimeSeatIds,
            @NotNull PaymentMethod paymentMethod) {}

    private static class BookingProblem extends RuntimeException {
        final HttpStatus status;
        BookingProblem(HttpStatus status, String message) { super(message); this.status = status; }
    }

    @ExceptionHandler(BookingProblem.class)
    public ResponseEntity<Map<String, String>> bookingProblem(BookingProblem problem) {
        return ResponseEntity.status(problem.status).body(Map.of("message", problem.getMessage()));
    }

    @GetMapping("/getShowtimeSeats")
    @Transactional(readOnly = true)
    public List<ShowtimeSeatView> getShowtimeSeats(@RequestParam Long showtimeId) {
        if (em.find(Showtime.class, showtimeId) == null)
            throw new BookingProblem(HttpStatus.NOT_FOUND, "Không tìm thấy suất chiếu.");
        return em.createQuery("select ss from ShowtimeSeat ss join fetch ss.seat "
                        + "where ss.showtime.showtimeId = :id order by ss.seat.seatRow, ss.seat.seatNumber",
                        ShowtimeSeat.class)
                .setParameter("id", showtimeId).getResultList().stream()
                .map(ss -> new ShowtimeSeatView(ss.getShowtimeSeatId(), ss.getSeat().getSeatId(),
                        ss.getSeat().getSeatRow(), ss.getSeat().getSeatNumber(),
                        ss.getSeat().getSeatType(), ss.getStatus()))
                .toList();
    }

    @PostMapping("/createBooking")
    @Transactional
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingInput input) {
        if (input.paymentMethod() != PaymentMethod.CASH)
            throw new BookingProblem(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ thanh toán tại quầy.");
        if (new HashSet<>(input.showtimeSeatIds()).size() != input.showtimeSeatIds().size())
            throw new BookingProblem(HttpStatus.BAD_REQUEST, "Danh sách ghế bị trùng.");
        User user = em.find(User.class, input.userId());
        if (user == null) throw new BookingProblem(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng.");
        Showtime showtime = em.find(Showtime.class, input.showtimeId());
        if (showtime == null) throw new BookingProblem(HttpStatus.NOT_FOUND, "Không tìm thấy suất chiếu.");
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED
                || !showtime.getStartTime().isAfter(OffsetDateTime.now()))
            throw new BookingProblem(HttpStatus.CONFLICT, "Suất chiếu đã bắt đầu hoặc không còn nhận đặt vé.");
        if (showtime.getBasePrice() == null || showtime.getBasePrice().signum() <= 0)
            throw new BookingProblem(HttpStatus.BAD_REQUEST, "Giá vé của suất chiếu chưa hợp lệ.");

        // Acquire row locks in a stable order. Competing bridge requests cannot book the same seat.
        List<ShowtimeSeat> selected = new ArrayList<>();
        for (Long id : input.showtimeSeatIds().stream().sorted().toList()) {
            ShowtimeSeat seat = em.find(ShowtimeSeat.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (seat == null || !seat.getShowtime().getShowtimeId().equals(input.showtimeId())
                    || !seat.getSeat().getRoom().getRoomId().equals(showtime.getRoom().getRoomId()))
                throw new BookingProblem(HttpStatus.BAD_REQUEST, "Ghế không thuộc suất chiếu hoặc phòng đã chọn.");
            if (seat.getStatus() != SeatStatus.AVAILABLE)
                throw new BookingProblem(HttpStatus.CONFLICT, "Một ghế đã được đặt hoặc đang giữ. Hãy làm mới sơ đồ ghế.");
            selected.add(seat);
        }

        BigDecimal price = showtime.getBasePrice();
        OffsetDateTime now = OffsetDateTime.now();
        Booking booking = Booking.builder().user(user).showtime(showtime)
                .bookingCode("BK-" + UUID.randomUUID().toString().replace("-", "").toUpperCase())
                .totalAmount(price.multiply(BigDecimal.valueOf(selected.size())))
                .status(BookingStatus.PENDING).paymentMethod(PaymentMethod.CASH).bookingTime(now).build();
        em.persist(booking);
        for (ShowtimeSeat seat : selected) {
            seat.setStatus(SeatStatus.BOOKED);
            seat.setUpdatedAt(now);
            em.persist(BookingDetail.builder().booking(booking).showtimeSeat(seat)
                    .price(price).createdAt(now).build());
        }
        em.flush();
        return new BookingResponse(booking.getBookingId(), user.getUserId(), showtime.getShowtimeId(),
                booking.getBookingCode(), booking.getTotalAmount(), booking.getStatus(),
                booking.getPaymentMethod(), booking.getBookingTime());
    }
}
