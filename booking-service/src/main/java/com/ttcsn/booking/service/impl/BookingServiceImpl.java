package com.ttcsn.booking.service.impl;

import com.ttcsn.booking.client.MovieClient;
import com.ttcsn.booking.client.ShowtimeClient;
import com.ttcsn.booking.client.UserClient;
import com.ttcsn.booking.client.VenueClient;
import com.ttcsn.booking.dto.request.BookingCreateRequest;
import com.ttcsn.booking.dto.request.BookingUpdateRequest;
import com.ttcsn.booking.dto.response.BookingResponse;
import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.entity.BookingDetail;
import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.exception.BookingNotFoundException;
import com.ttcsn.booking.exception.SeatAlreadyBookedException;
import com.ttcsn.booking.repository.BookingDetailRepository;
import com.ttcsn.booking.repository.BookingRepository;
import com.ttcsn.booking.service.BookingService;
import com.ttcsn.common.dto.SeatIdsRequest;
import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SOA: createBooking không còn 1-big-transaction.
 * - Kiểm tra user qua user-service (Feign)
 * - Giữ ghế (HOLD -> BOOKED) qua movie-service (Feign, có optimistic check bên service đó)
 * - Booking + BookingDetail là transaction cục bộ của booking-service
 * (Saga orchestration + outbox + TTL sẽ bổ sung từ Step 5 theo SOA_REFACTOR_PLAN.md §2.4)
 *
 * @author quanghieu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final UserClient userClient;
    private final MovieClient movieClient;    // SOA: đọc thông tin Phim/Suất chiếu (sync)
    private final VenueClient venueClient;    // SOA: kiểm tra phòng chiếu (sync)
    private final ShowtimeClient showtimeClient; // internal: hold/release ghế (sync)

    @Override
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        return convertToResponse(booking);
    }

    @Override
    public List<BookingResponse> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponse> getBookingsByShowtimeId(Long showtimeId) {
        return bookingRepository.findByShowtimeId(showtimeId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        // 1. Kiểm tra user tồn tại (user-service)
        if (!userClient.userExists(request.userId())) {
            throw new NotFoundException("Không tìm thấy người dùng với userId = " + request.userId());
        }

        // 2. Đọc thông tin Suất chiếu từ movie-service (OpenFeign sync)
        com.ttcsn.common.dto.ShowtimeView showtime;
        try {
            showtime = movieClient.getShowtimeById(request.showtimeId());
        } catch (feign.FeignException.NotFound ex) {
            throw new NotFoundException("Không tìm thấy suất chiếu với showtimeId = " + request.showtimeId());
        }
        if (showtime == null) {
            throw new NotFoundException("Không tìm thấy suất chiếu với showtimeId = " + request.showtimeId());
        }

        // 3. Kiểm tra phòng chiếu còn hợp lệ (venue-service)
        if (showtime.roomId() != null && venueClient.getRoom(showtime.roomId()) == null) {
            throw new NotFoundException("Không tìm thấy phòng chiếu với roomId = " + showtime.roomId());
        }

        // 4. Giữ các ghế (movie-service cập nhật SeatStatus trong internal transaction)
        SeatHoldResult hold = showtimeClient.holdSeats(
                new SeatIdsRequest(request.showtimeId(), request.showtimeSeatIds()));
        if (!hold.success()) {
            throw new SeatAlreadyBookedException("Một hoặc nhiều ghế đã được đặt bởi người khac: "
                    + String.join(", ", hold.unavailableSeatIds().stream().map(String::valueOf).toList()));
        }

        try {
            // 4. Booking + BookingDetail (transaction cục bộ)
            String generatedBookingCode = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Booking booking = Booking.builder()
                    .bookingCode(generatedBookingCode)
                    .totalAmount(request.totalAmount())
                    .status(BookingStatus.PENDING)
                    .paymentMethod(request.paymentMethod())
                    .bookingTime(OffsetDateTime.now())
                    .userId(request.userId())
                    .showtimeId(request.showtimeId())
                    .build();
            Booking savedBooking = bookingRepository.save(booking);

            BigDecimal pricePerSeat = request.totalAmount()
                    .divide(BigDecimal.valueOf(request.showtimeSeatIds().size()), 2, RoundingMode.HALF_UP);
            List<BookingDetail> details = request.showtimeSeatIds().stream()
                    .map(seatId -> BookingDetail.builder()
                            .bookingId(savedBooking.getBookingId())
                            .showtimeSeatId(seatId)
                            .price(pricePerSeat)
                            .createdAt(OffsetDateTime.now())
                            .build())
                    .collect(Collectors.toList());
            bookingDetailRepository.saveAll(details);

            return convertToResponse(savedBooking);

        } catch (RuntimeException ex) {
            // 5. Bỏ transaction sẽ được thay bằng Saga orchestration (Step 5) — bù trừ ghế ngay bây giờ
            showtimeClient.releaseSeats(new SeatIdsRequest(request.showtimeId(), request.showtimeSeatIds()));
            throw ex;
        }
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(Long id, BookingUpdateRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (request.status() != null && request.status() != booking.getStatus()) {
            booking.setStatus(request.status());
            if (request.status() == BookingStatus.CANCELLED || request.status() == BookingStatus.EXPIRED) {
                releaseBookingSeats(id, booking.getShowtimeId());
            }
        }

        Booking updatedBooking = bookingRepository.save(booking);
        return convertToResponse(updatedBooking);
    }

    private void releaseBookingSeats(Long bookingId, Long showtimeId) {
        List<Long> showtimeSeatIds = bookingDetailRepository.findByBookingId(bookingId).stream()
                .map(BookingDetail::getShowtimeSeatId)
                .toList();
        try {
            showtimeClient.releaseSeats(new SeatIdsRequest(showtimeId, showtimeSeatIds));
            log.info("[booking-service] Đã trả ghế về AVAILABLE cho booking {} (bù trừ)", bookingId);
        } catch (RuntimeException ex) {
            log.error("[booking-service] KHÔNG trả được ghế về AVAILABLE cho booking {}: {}",
                    bookingId, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new BookingNotFoundException(id);
        }
        bookingRepository.deleteById(id);
    }

    private BookingResponse convertToResponse(Booking booking) {
        return new BookingResponse(
                booking.getBookingId(),
                booking.getUserId(),
                booking.getShowtimeId(),
                booking.getBookingCode(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getPaymentMethod(),
                booking.getBookingTime()
        );
    }
}
