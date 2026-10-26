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
import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.common.dto.SeatIdsRequest;
import com.ttcsn.common.dto.ShowtimeView;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.ForbiddenException;
import com.ttcsn.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SOA: createBooking không còn 1-big-transaction.
 * - Kiểm tra user qua user-service (Feign)
 * - Giữ ghế (HOLD -> BOOKED) qua movie-service (Feign, có optimistic check bên service đó)
 * - Booking + BookingDetail là transaction cục bộ của booking-service
 *
 * <p>Authorization: userId/role lấy từ header do gateway gắn, không tin body/query.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final String ADMIN = "ADMIN";

    /**
     * Chuyển trạng thái được phép qua API.
     * PENDING -> CANCELLED (CUSTOMER/ADMIN), PENDING -> EXPIRED (ADMIN).
     * PAID chỉ do PaymentCompletedListener đặt; PAID/CANCELLED/EXPIRED là trạng thái cuối.
     */
    private static final Map<BookingStatus, Set<BookingStatus>> API_TRANSITIONS = Map.of(
            BookingStatus.PENDING, Set.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED),
            BookingStatus.PAID, Set.of(),
            BookingStatus.CANCELLED, Set.of(),
            BookingStatus.EXPIRED, Set.of()
    );

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final UserClient userClient;
    private final MovieClient movieClient;    // SOA: đọc thông tin Phim/Suất chiếu (sync)
    private final VenueClient venueClient;    // SOA: kiểm tra phòng chiếu (sync)
    private final ShowtimeClient showtimeClient; // internal: hold/release ghế (sync)

    @Override
    public List<BookingResponse> getAllBookings(String role) {
        requireAdmin(role);
        return bookingRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponse getBookingById(Long id, Long userId, String role) {
        return convertToResponse(findOwnedBooking(id, userId, role));
    }

    @Override
    public List<BookingResponse> getBookingsByUserId(Long requestedUserId, Long currentUserId, String role) {
        Long targetUserId = ADMIN.equals(role) && requestedUserId != null
                ? requestedUserId
                : currentUserId;
        return bookingRepository.findByUserId(targetUserId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponse> getBookingsByShowtimeId(Long showtimeId, String role) {
        requireAdmin(role);
        return bookingRepository.findByShowtimeId(showtimeId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, Long userId) {
        // 1. Kiểm tra user tồn tại (user-service)
        if (!userClient.userExists(userId)) {
            throw new NotFoundException("Không tìm thấy người dùng với userId = " + userId);
        }

        // 2. Đọc thông tin Suất chiếu từ movie-service (OpenFeign sync)
        ShowtimeView showtime;
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

        // 4. Tính tiền ở server; client không gửi userId/totalAmount/holdBy.
        if (request.seatIds() == null) {
            throw new IllegalArgumentException("Danh sách ghế đặt không được để trống");
        }
        List<Long> seatIds = request.seatIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (seatIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách ghế đặt không được để trống");
        }
        BigDecimal basePrice = showtime.basePrice();
        if (basePrice == null || basePrice.signum() <= 0) {
            throw new ConflictException("Suất chiếu chưa có giá vé hợp lệ");
        }
        BigDecimal totalAmount = basePrice.multiply(BigDecimal.valueOf(seatIds.size()));
        String holdBy = String.valueOf(userId);

        // 5. Chốt các ghế (movie-service kiểm tra ghế còn trống / đúng người giữ)
        SeatHoldResult hold = showtimeClient.holdSeats(
                new SeatIdsRequest(request.showtimeId(), seatIds, holdBy));
        if (!hold.success()) {
            throw new SeatAlreadyBookedException("Một hoặc nhiều ghế đã được đặt bởi người khác: "
                    + String.join(", ", hold.unavailableSeatIds().stream().map(String::valueOf).toList()));
        }

        try {
            // 6. Booking + BookingDetail (transaction cục bộ)
            String generatedBookingCode = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Booking booking = Booking.builder()
                    .bookingCode(generatedBookingCode)
                    .totalAmount(totalAmount)
                    .status(BookingStatus.PENDING)
                    .paymentMethod(request.paymentMethod())
                    .bookingTime(OffsetDateTime.now())
                    .userId(userId)
                    .showtimeId(request.showtimeId())
                    .build();
            Booking savedBooking = bookingRepository.save(booking);

            List<BookingDetail> details = seatIds.stream()
                    .map(showtimeSeatId -> BookingDetail.builder()
                            .booking(savedBooking)
                            .showtimeSeatId(showtimeSeatId)
                            .price(basePrice)
                            .createdAt(OffsetDateTime.now())
                            .build())
                    .collect(Collectors.toList());
            bookingDetailRepository.saveAll(details);

            return convertToResponse(savedBooking);

        } catch (RuntimeException ex) {
            // 7. Bù trừ ghế ngay khi transaction cục bộ fail
            showtimeClient.releaseSeats(new SeatIdsRequest(request.showtimeId(), seatIds, holdBy));
            throw ex;
        }
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(Long id, BookingUpdateRequest request, Long userId, String role) {
        Booking booking = findOwnedBooking(id, userId, role);
        BookingStatus target = request.status();
        if (target == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }
        // PAID chỉ do payment listener đặt, không nhận từ API (kể cả ADMIN).
        if (target == BookingStatus.PAID) {
            throw new IllegalStateException("PAID chỉ được đặt bởi payment listener");
        }
        if (target == booking.getStatus()) {
            return convertToResponse(booking);
        }
        if (!API_TRANSITIONS.getOrDefault(booking.getStatus(), Set.of()).contains(target)) {
            throw new IllegalStateException(
                    "Không thể chuyển booking từ " + booking.getStatus() + " sang " + target);
        }
        if (!ADMIN.equals(role) && target != BookingStatus.CANCELLED) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
        }

        booking.setStatus(target);
        if (target == BookingStatus.CANCELLED || target == BookingStatus.EXPIRED) {
            releaseBookingSeats(id, booking.getShowtimeId());
        }

        Booking updatedBooking = bookingRepository.save(booking);
        return convertToResponse(updatedBooking);
    }

    private void releaseBookingSeats(Long bookingId, Long showtimeId) {
        List<Long> seatIds = bookingDetailRepository.findByBooking_BookingId(bookingId).stream()
                .map(BookingDetail::getShowtimeSeatId)
                .toList();
        try {
            showtimeClient.releaseSeats(new SeatIdsRequest(showtimeId, seatIds, null));
            log.info("[booking-service] Đã trả ghế về AVAILABLE cho booking {} (bù trừ)", bookingId);
        } catch (RuntimeException ex) {
            log.error("[booking-service] KHÔNG trả được ghế về AVAILABLE cho booking {}: {}",
                    bookingId, ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteBooking(Long id, Long userId, String role) {
        Booking booking = findOwnedBooking(id, userId, role);
        // Chỉ booking PENDING còn giữ ghế BOOKED; PAID/CANCELLED/EXPIRED không được trả ghế
        // để tránh giải phóng nhầm ghế đã bán hoặc đã được người khác đặt lại.
        if (booking.getStatus() == BookingStatus.PENDING) {
            releaseBookingSeats(id, booking.getShowtimeId());
        }
        bookingRepository.delete(booking);
    }

    @Override
    public BookingPaymentView getPaymentView(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        return new BookingPaymentView(
                booking.getBookingId(),
                booking.getUserId(),
                booking.getTotalAmount(),
                booking.getStatus() != null ? booking.getStatus().name() : null
        );
    }

    private Booking findOwnedBooking(Long id, Long userId, String role) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        if (!ADMIN.equals(role) && !Objects.equals(booking.getUserId(), userId)) {
            // 404 để không lộ booking của người khác có tồn tại hay không.
            throw new BookingNotFoundException(id);
        }
        return booking;
    }

    private static void requireAdmin(String role) {
        if (!ADMIN.equals(role)) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
        }
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
