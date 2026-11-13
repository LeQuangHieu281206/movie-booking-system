package com.ttcsn.booking.service.impl;

import com.ttcsn.booking.client.MovieClient;
import com.ttcsn.booking.client.ShowtimeClient;
import com.ttcsn.booking.client.UserClient;
import com.ttcsn.booking.client.VenueClient;
import com.ttcsn.booking.dto.request.BookingCreateRequest;
import com.ttcsn.booking.dto.request.BookingUpdateRequest;
import com.ttcsn.booking.dto.response.BookingResponse;
import com.ttcsn.booking.dto.response.BookingStatsResponse;
import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.entity.BookingDetail;
import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.exception.BookingNotFoundException;
import com.ttcsn.booking.exception.SeatAlreadyBookedException;
import com.ttcsn.booking.repository.BookingDetailRepository;
import com.ttcsn.booking.repository.BookingRepository;
import com.ttcsn.booking.service.BookingSeatReleaser;
import com.ttcsn.booking.service.BookingService;
import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.common.dto.SeatIdsRequest;
import com.ttcsn.common.dto.SeatDto;
import com.ttcsn.common.dto.ShowtimeView;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.ForbiddenException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.common.security.Roles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Kỳ xem doanh số: mỗi kỳ là MỘT đơn vị thời gian đang diễn ra
     * (hôm nay / tháng này / năm nay). Biểu đồ chia nhỏ đơn vị đó thành các mốc:
     * ngày -> 24 giờ, tháng -> từng ngày, năm -> 12 tháng (giờ Việt Nam).
     */
    private enum StatsPeriod {
        DAY("day"),
        MONTH("month"),
        YEAR("year");

        private final String value;

        StatsPeriod(String value) {
            this.value = value;
        }

        /** Không truyền period thì mặc định xem hôm nay. */
        static StatsPeriod from(String raw) {
            if (raw == null || raw.isBlank()) {
                return DAY;
            }
            for (StatsPeriod period : values()) {
                if (period.value.equalsIgnoreCase(raw.trim())) {
                    return period;
                }
            }
            throw new IllegalArgumentException("period chỉ nhận day, month hoặc year");
        }

        /** Mốc bắt đầu của kỳ, đã hạ về đầu ngày/tháng/năm. */
        LocalDate windowStart(LocalDate today) {
            return switch (this) {
                case DAY -> today;
                case MONTH -> today.withDayOfMonth(1);
                case YEAR -> today.withDayOfYear(1);
            };
        }

        /**
         * Đủ mọi mốc của kỳ, kể cả mốc chưa phát sinh doanh thu, để biểu đồ
         * không co giãn khi thiếu dữ liệu.
         */
        List<String> keys(LocalDate today) {
            return switch (this) {
                case DAY -> {
                    String day = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    List<String> keys = new ArrayList<>(24);
                    for (int hour = 0; hour < 24; hour++) {
                        keys.add("%s %02d".formatted(day, hour));
                    }
                    yield keys;
                }
                case MONTH -> {
                    List<String> keys = new ArrayList<>(today.lengthOfMonth());
                    for (int day = 1; day <= today.lengthOfMonth(); day++) {
                        keys.add(today.withDayOfMonth(day).format(DateTimeFormatter.ISO_LOCAL_DATE));
                    }
                    yield keys;
                }
                case YEAR -> {
                    List<String> keys = new ArrayList<>(12);
                    for (int month = 1; month <= 12; month++) {
                        keys.add(today.withMonth(month).format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    }
                    yield keys;
                }
            };
        }
    }

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
    private final BookingSeatReleaser seatReleaser;
    private final UserClient userClient;
    private final MovieClient movieClient;    // SOA: đọc thông tin Phim/Suất chiếu (sync)
    private final VenueClient venueClient;    // SOA: kiểm tra phòng chiếu (sync)
    private final ShowtimeClient showtimeClient; // internal: hold/release ghế (sync)

    @Override
    public List<BookingResponse> getAllBookings(String role) {
        requireAdmin(role);
        return convertToResponses(bookingRepository.findAll());
    }

    @Override
    public BookingResponse getBookingById(Long id, Long userId, String role) {
        return convertToResponse(findOwnedBooking(id, userId, role));
    }

    @Override
    public List<BookingResponse> getBookingsByUserId(Long requestedUserId, Long currentUserId, String role) {
        Long targetUserId = Roles.isAdmin(role) && requestedUserId != null
                ? requestedUserId
                : currentUserId;
        return convertToResponses(bookingRepository.findByUserId(targetUserId));
    }

    @Override
    public List<BookingResponse> getBookingsByShowtimeId(Long showtimeId, String role) {
        requireAdmin(role);
        return convertToResponses(bookingRepository.findByShowtimeId(showtimeId));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingStatsResponse getBookingStats(String role, String period) {
        requireAdmin(role);
        StatsPeriod statsPeriod = StatsPeriod.from(period);
        LocalDate today = LocalDate.now(VN_ZONE);
        OffsetDateTime from = statsPeriod.windowStart(today).atStartOfDay(VN_ZONE).toOffsetDateTime();

        List<Object[]> rows = switch (statsPeriod) {
            case DAY -> bookingRepository.sumPaidRevenueByHour(from);
            case MONTH -> bookingRepository.sumPaidRevenueByDay(from);
            case YEAR -> bookingRepository.sumPaidRevenueByMonth(from);
        };

        Map<String, BookingStatsResponse.RevenueBucket> byKey = new HashMap<>();
        for (Object[] row : rows) {
            String key = row[0].toString();
            byKey.put(key, new BookingStatsResponse.RevenueBucket(
                    key,
                    (BigDecimal) row[1],
                    ((Number) row[2]).longValue()
            ));
        }

        List<BookingStatsResponse.RevenueBucket> revenueSeries = new ArrayList<>();
        for (String key : statsPeriod.keys(today)) {
            revenueSeries.add(byKey.getOrDefault(key,
                    new BookingStatsResponse.RevenueBucket(key, BigDecimal.ZERO, 0)));
        }

        // Ba con số tổng hợp tính trên đúng kỳ đang xem để khớp với biểu đồ.
        BigDecimal revenue = revenueSeries.stream()
                .map(BookingStatsResponse.RevenueBucket::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidBookings = revenueSeries.stream()
                .mapToLong(BookingStatsResponse.RevenueBucket::paidBookings)
                .sum();

        return new BookingStatsResponse(
                revenue,
                paidBookings,
                bookingDetailRepository.countPaidSeatsSince(from),
                statsPeriod.value,
                revenueSeries
        );
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request, Long userId) {
        if (!userClient.userExists(userId)) {
            throw new NotFoundException("Không tìm thấy người dùng với userId = " + userId);
        }

        ShowtimeView showtime;
        try {
            showtime = movieClient.getShowtimeById(request.showtimeId());
        } catch (feign.FeignException.NotFound ex) {
            throw new NotFoundException("Không tìm thấy suất chiếu với showtimeId = " + request.showtimeId());
        }

        if (showtime.roomId() != null && venueClient.getRoom(showtime.roomId()) == null) {
            throw new NotFoundException("Không tìm thấy phòng chiếu với roomId = " + showtime.roomId());
        }

        // Client không gửi userId/totalAmount/holdBy; server tự tính.
        List<Long> seatIds = request.normalizedSeatIds();
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
                    .map(seatId -> {
                        Long showtimeSeatId = hold.seatToShowtimeSeatId().get(seatId);
                        if (showtimeSeatId == null) {
                            throw new IllegalStateException(
                                    "Không lấy được showtimeSeatId cho ghế " + seatId);
                        }
                        return BookingDetail.builder()
                                .booking(savedBooking)
                                .seatId(seatId)
                                .showtimeSeatId(showtimeSeatId)
                                .price(basePrice)
                                .createdAt(OffsetDateTime.now())
                                .build();
                    })
                    .collect(Collectors.toList());
            bookingDetailRepository.saveAll(details);

            return convertToResponse(savedBooking);

        } catch (RuntimeException ex) {
            // Bù trừ ngay khi transaction cục bộ fail, trước khi exception thoát ra.
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
            throw new ConflictException("PAID chỉ được đặt bởi payment listener");
        }
        if (target == booking.getStatus()) {
            return convertToResponse(booking);
        }
        if (!API_TRANSITIONS.getOrDefault(booking.getStatus(), Set.of()).contains(target)) {
            throw new ConflictException(
                    "Không thể chuyển booking từ " + booking.getStatus() + " sang " + target);
        }
        if (!Roles.isAdmin(role) && target != BookingStatus.CANCELLED) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
        }

        booking.setStatus(target);
        if (target == BookingStatus.CANCELLED || target == BookingStatus.EXPIRED) {
            seatReleaser.release(booking);
        }

        Booking updatedBooking = bookingRepository.save(booking);
        return convertToResponse(updatedBooking);
    }

    /**
     * Hết hạn booking PENDING quá hạn chờ thanh toán (scheduler định kỳ gọi).
     * Giữ ghế BOOKED mãi nếu user đóng trang không thanh toán — nhả ghế để khách khác đặt.
     * Phải lớn hơn thời gian giữ chỗ tạm (HOLD 10 phút ở movie-service).
     */
    @Value("${app.booking.pending-expiry-minutes:15}")
    private long pendingExpiryMinutes;

    @Override
    @Transactional
    public int expireStalePendingBookings() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(pendingExpiryMinutes);
        List<Booking> stale = bookingRepository
                .findByStatusAndBookingTimeBefore(BookingStatus.PENDING, cutoff);

        int expired = 0;
        for (Booking booking : stale) {
            // Guarded update: chỉ nhả ghế khi chính mình đưa được booking ra khỏi PENDING.
            // Nếu event thanh toán (SUCCESS/FAILED) xử lý trước đó thì trạng thái đã đổi,
            // update ảnh hưởng 0 dòng -> bỏ qua, tuyệt đối không nhả ghế của đơn đã PAID.
            if (bookingRepository.expireIfPending(booking.getBookingId()) == 0) {
                log.info("[booking-service] Booking #{} đã đổi trạng thái trước khi hết hạn — bỏ qua",
                        booking.getBookingId());
                continue;
            }
            seatReleaser.release(booking);
            expired++;
            log.info("[booking-service] Booking #{} quá hạn {} phút chưa thanh toán -> EXPIRED (đã nhả ghế)",
                    booking.getBookingId(), pendingExpiryMinutes);
        }
        return expired;
    }

    @Override
    @Transactional
    public void deleteBooking(Long id, Long userId, String role) {
        Booking booking = findOwnedBooking(id, userId, role);
        // Chỉ booking PENDING còn giữ ghế BOOKED; PAID/CANCELLED/EXPIRED không được trả ghế
        // để tránh giải phóng nhầm ghế đã bán hoặc đã được người khác đặt lại.
        if (booking.getStatus() == BookingStatus.PENDING) {
            seatReleaser.release(booking);
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
        if (!Roles.isAdmin(role) && !Objects.equals(booking.getUserId(), userId)) {
            // 404 để không lộ booking của người khác có tồn tại hay không.
            throw new BookingNotFoundException(id);
        }
        return booking;
    }

    private static void requireAdmin(String role) {
        if (!Roles.isAdmin(role)) {
            throw new ForbiddenException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    private BookingResponse convertToResponse(Booking booking) {
        return convertToResponses(List.of(booking)).get(0);
    }

    /** Gắn vị trí ghế cho cả danh sách vé: 1 query booking_details + 1 lần gọi venue-service. */
    private List<BookingResponse> convertToResponses(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }
        List<Long> bookingIds = bookings.stream().map(Booking::getBookingId).toList();
        Map<Long, List<Long>> seatIdsByBooking = bookingDetailRepository
                .findByBooking_BookingIdIn(bookingIds).stream()
                .collect(Collectors.groupingBy(
                        detail -> detail.getBooking().getBookingId(),
                        Collectors.mapping(BookingDetail::getSeatId, Collectors.toList())));
        Map<Long, String> labelsById = seatLabels(seatIdsByBooking.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList());
        return bookings.stream()
                .map(booking -> {
                    List<String> labels = seatIdsByBooking
                            .getOrDefault(booking.getBookingId(), List.of()).stream()
                            .map(labelsById::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return new BookingResponse(
                            booking.getBookingId(),
                            booking.getUserId(),
                            booking.getShowtimeId(),
                            booking.getBookingCode(),
                            booking.getTotalAmount(),
                            booking.getStatus(),
                            booking.getPaymentMethod(),
                            booking.getBookingTime(),
                            labels
                    );
                })
                .toList();
    }

    /** venue-service có thể tạm lỗi; vé vẫn phải xem được, chỉ thiếu vị trí ghế. */
    private Map<Long, String> seatLabels(List<Long> seatIds) {
        if (seatIds.isEmpty()) {
            return Map.of();
        }
        try {
            return venueClient.getSeatsByIds(seatIds).stream()
                    .collect(Collectors.toMap(
                            SeatDto::seatId,
                            seat -> seat.seatRow() + seat.seatNumber(),
                            (first, second) -> first));
        } catch (RuntimeException ex) {
            log.warn("[booking-service] Không lấy được vị trí ghế từ venue-service: {}", ex.getMessage());
            return Map.of();
        }
    }
}
