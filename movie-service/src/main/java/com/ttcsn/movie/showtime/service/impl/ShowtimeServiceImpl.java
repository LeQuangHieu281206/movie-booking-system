package com.ttcsn.movie.showtime.service.impl;

import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatDto;
import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.movie.client.VenueClient;
import com.ttcsn.movie.dto.response.MovieResponse;
import com.ttcsn.movie.entity.Movie;
import com.ttcsn.movie.exception.MovieNotFoundException;
import com.ttcsn.movie.repository.MovieRepository;
import com.ttcsn.movie.showtime.dto.request.ShowtimeRequest;
import com.ttcsn.movie.showtime.dto.response.ShowtimeSeatView;
import com.ttcsn.movie.showtime.dto.response.ShowtimeResponse;
import com.ttcsn.movie.showtime.entity.Showtime;
import com.ttcsn.movie.showtime.entity.ShowtimeSeat;
import com.ttcsn.movie.showtime.entity.enums.SeatStatus;
import com.ttcsn.movie.showtime.entity.enums.ShowtimeStatus;
import com.ttcsn.movie.showtime.exception.ShowtimeNotFoundException;
import com.ttcsn.movie.showtime.repository.ShowtimeRepository;
import com.ttcsn.movie.showtime.repository.ShowtimeSeatRepository;
import com.ttcsn.movie.showtime.service.ShowtimeService;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author quanghieu
 */
@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(10);
    private static final long SEATS_CACHE_TTL_MS = 60_000;

    /** Giữ ghế: 1 lượt truy vấn duy nhất (kiểm tra suất + chèn/cập nhật có điều kiện + trả id). */
    private static final String HOLD_SEAT_SQL = """
            WITH allowed_showtime AS (
                SELECT showtime_id FROM showtimes
                WHERE showtime_id = :showtimeId AND status = 'SCHEDULED' AND start_time > now()
            )
            INSERT INTO showtime_seats (showtime_id, seat_id, status, hold_until, hold_by, updated_at)
            SELECT allowed_showtime.showtime_id, :seatId, 'HOLDING', :holdUntil, :holdBy, now() FROM allowed_showtime
            ON CONFLICT (showtime_id, seat_id) DO UPDATE
                SET status = 'HOLDING', hold_until = EXCLUDED.hold_until,
                    hold_by = EXCLUDED.hold_by, updated_at = now()
                WHERE showtime_seats.status = 'AVAILABLE'
                   OR (showtime_seats.status = 'HOLDING'
                       AND (showtime_seats.hold_by = EXCLUDED.hold_by OR showtime_seats.hold_until <= now()))
            RETURNING showtime_seat_id
            """;

    /** Nhả ghế đang giữ: 1 DELETE có điều kiện (chỉ người giữ hoặc giữ đã hết hạn). */
    private static final String RELEASE_SEAT_SQL = """
            WITH deleted AS (
                DELETE FROM showtime_seats
                WHERE showtime_id = :showtimeId AND seat_id = :seatId
                  AND status = 'HOLDING' AND (hold_by = :holdBy OR hold_until <= now())
                RETURNING showtime_seat_id
            )
            SELECT count(*) FROM deleted
            """;

    /** Chốt vé BOOKED hàng loạt: 1 lượt chèn/cập nhật cho cả danh sách ghế. */
    private static final String BOOK_SEATS_SQL = """
            WITH allowed_showtime AS (
                SELECT showtime_id FROM showtimes WHERE showtime_id = :showtimeId
            )
            INSERT INTO showtime_seats (showtime_id, seat_id, status, hold_until, hold_by, updated_at)
            SELECT allowed_showtime.showtime_id, x.seat_id, 'BOOKED', NULL, NULL, now()
            FROM allowed_showtime JOIN unnest(array[LITERAL_SEAT_IDS]) AS x(seat_id) ON TRUE
            ON CONFLICT (showtime_id, seat_id) DO UPDATE
                SET status = 'BOOKED', hold_until = NULL, hold_by = NULL, updated_at = now()
                WHERE showtime_seats.status = 'AVAILABLE'
                   OR (showtime_seats.status = 'HOLDING'
                       AND (showtime_seats.hold_by = :holdBy OR showtime_seats.hold_until <= now()))
            RETURNING seat_id
            """;

    /** Bù trừ: trả một nhóm ghế về AVAILABLE trong 1 lượt. */
    private static final String RELEASE_SEATS_INTERNAL_SQL = """
            UPDATE showtime_seats
            SET status = 'AVAILABLE', hold_until = NULL, hold_by = NULL, updated_at = now()
            WHERE showtime_id = :showtimeId AND seat_id = ANY(array[LITERAL_SEAT_IDS])
            """;

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final VenueClient venueClient; // SOA: thay cho Room/Seat repository của monolith

    @PersistenceContext
    private EntityManager em;

    private final Map<Long, RoomSeatsCache> seatsByRoomCache = new ConcurrentHashMap<>();

    private record RoomSeatsCache(List<SeatDto> seats, long expiresAtMs) {}

    /** Sơ đồ ghế vật lý của phòng hiếm khi đổi — tái dùng 60s, tránh 1 lượt Feign mỗi lần mở trang ghế. */
    private List<SeatDto> getSeatsByRoomCached(Long roomId) {
        long now = System.currentTimeMillis();
        RoomSeatsCache cached = seatsByRoomCache.get(roomId);
        if (cached != null && now < cached.expiresAtMs()) {
            return cached.seats();
        }
        List<SeatDto> fresh = venueClient.getSeatsByRoom(roomId);
        seatsByRoomCache.put(roomId, new RoomSeatsCache(fresh, now + SEATS_CACHE_TTL_MS));
        return fresh;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getAllShowtimes() {
        List<Showtime> showtimes = showtimeRepository.findAll();
        Map<Long, RoomDto> rooms = loadRooms(showtimes); // 1 lần cho toàn bộ phòng — tránh 1 Feign call / suất
        return showtimes.stream()
                .map(showtime -> toResponse(showtime, rooms.get(showtime.getRoomId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByMovieId(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFoundException(movieId);
        }
        List<Showtime> showtimes = showtimeRepository.findAllWithMovieByMovieId(movieId);
        Map<Long, RoomDto> rooms = loadRooms(showtimes);
        return showtimes.stream()
                .map(showtime -> toResponse(showtime, rooms.get(showtime.getRoomId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(Long showtimeId) {
        return toResponse(findShowtimeOrThrow(showtimeId));
    }

    /**
     * Mô hình showtime_seats "phát sinh theo tương tác": bảng không chèn sẵn ghế khi tạo suất.
     * Ghế vật lý của phòng đọc từ venue-service; bản ghi showtime_seats chỉ tồn tại
     * khi ghế đang HOLDING (giữ chỗ có hạn) hoặc đã BOOKED (chốt vé).
     * Ghế không có bản ghi, hoặc HOLDING đã quá holdUntil, coi như AVAILABLE (lọc khi truy vấn).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeSeatView> getShowtimeSeats(Long showtimeId) {
        Showtime showtime = findShowtimeOrThrow(showtimeId);
        List<SeatDto> physicalSeats = getSeatsByRoomCached(showtime.getRoomId());
        Map<Long, ShowtimeSeat> rowBySeatId = showtimeSeatRepository.findByShowtime_ShowtimeId(showtimeId).stream()
                .collect(Collectors.toMap(ShowtimeSeat::getSeatId, Function.identity()));
        OffsetDateTime now = OffsetDateTime.now();

        return physicalSeats.stream()
                .map(physical -> {
                    ShowtimeSeat row = rowBySeatId.get(physical.seatId());
                    String status = resolveStatus(row, now);
                    boolean hasRecord = !SeatStatus.AVAILABLE.name().equals(status) && row != null;
                    return new ShowtimeSeatView(
                            hasRecord ? row.getShowtimeSeatId() : null,
                            showtimeId,
                            physical.seatId(),
                            physical.seatRow(),
                            physical.seatNumber(),
                            physical.seatType(),
                            status,
                            hasRecord ? row.getHoldUntil() : null,
                            hasRecord ? row.getHoldBy() : null);
                })
                .sorted(Comparator
                        .comparing((ShowtimeSeatView v) -> v.seatRow() == null ? "" : v.seatRow())
                        .thenComparing(v -> v.seatNumber() == null ? Short.MAX_VALUE : v.seatNumber()))
                .toList();
    }

    /** Người dùng bấm chọn ghế: 1 lượt truy vấn gồm kiểm tra suất + chèn/refresh bản ghi HOLDING (10 phút). */
    @Override
    @Transactional
    public ShowtimeSeatView holdShowtimeSeat(Long showtimeId, Long seatId, String holdBy) {
        if (holdBy == null || holdBy.isBlank()) {
            throw new IllegalStateException("Thiếu định danh phiên giữ ghế (holdBy).");
        }
        OffsetDateTime holdUntil = OffsetDateTime.now().plus(HOLD_DURATION);
        List<?> rows = em.createNativeQuery(HOLD_SEAT_SQL)
                .setParameter("showtimeId", showtimeId)
                .setParameter("seatId", seatId)
                .setParameter("holdUntil", holdUntil)
                .setParameter("holdBy", holdBy)
                .getResultList();

        if (!rows.isEmpty()) { // chèn mới hoặc refresh/phủ của chính phiên giữ luôn có bản ghi trả về
            return new ShowtimeSeatView(((Number) rows.get(0)).longValue(), showtimeId, seatId,
                    null, null, null, SeatStatus.HOLDING.name(), holdUntil, holdBy);
        }
        // 0 dòng: hiếm gặp (xung đột/điều kiện suất) -> hỏi lại 1 lần để nêu đúng lý do
        throwHoldRejection(showtimeId, seatId, holdBy);
        throw new IllegalStateException("Ghế vừa được giữ bởi phiên khác. Chọn ghế khác.");
    }

    private void throwHoldRejection(Long showtimeId, Long seatId, String holdBy) {
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        if (showtime == null) {
            throw new IllegalStateException("Suất chiếu không tồn tại.");
        }
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new IllegalStateException("Suất chiếu không còn nhận đặt vé (trạng thái " + showtime.getStatus() + ").");
        }
        if (!showtime.getStartTime().isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("Suất chiếu đã bắt đầu, không thể giữ ghế.");
        }
        ShowtimeSeat row = showtimeSeatRepository.findByShowtime_ShowtimeIdAndSeatId(showtimeId, seatId).orElse(null);
        if (row == null) {
            throw new IllegalStateException("Ghế không thuộc sơ đồ của phòng chiếu này.");
        }
        if (row.getStatus() == SeatStatus.BOOKED) {
            throw new IllegalStateException("Ghế này đã được đặt thành công.");
        }
        if (!isHoldExpired(row, OffsetDateTime.now()) && !Objects.equals(holdBy, row.getHoldBy())) {
            throw new IllegalStateException("Ghế đang được khách khác giữ. Chọn ghế khác.");
        }
    }

    /** Bỏ giữ: 1 DELETE có điều kiện (chỉ người giữ hoặc giữ đã hết hạn). */
    @Override
    @Transactional
    public ShowtimeSeatView releaseShowtimeSeat(Long showtimeId, Long seatId, String holdBy) {
        Object deleted = em.createNativeQuery(RELEASE_SEAT_SQL)
                .setParameter("showtimeId", showtimeId)
                .setParameter("seatId", seatId)
                .setParameter("holdBy", holdBy)
                .getSingleResult();
        if (((Number) deleted).longValue() > 0) {
            return new ShowtimeSeatView(null, showtimeId, seatId,
                    null, null, null, SeatStatus.AVAILABLE.name(), null, null);
        }
        // Không xóa được -> phân biệt ghế chưa có giữ chỗ (bình thường) với ghế đã đặt / của người khác
        ShowtimeSeat row = showtimeSeatRepository.findByShowtime_ShowtimeIdAndSeatId(showtimeId, seatId).orElse(null);
        if (row == null || row.getStatus() == SeatStatus.AVAILABLE) {
            return new ShowtimeSeatView(null, showtimeId, seatId,
                    null, null, null, SeatStatus.AVAILABLE.name(), null, null);
        }
        if (row.getStatus() == SeatStatus.BOOKED) {
            throw new IllegalStateException("Ghế đã được đặt thành công, không thể bỏ giữ. Hủy vé để trả ghế.");
        }
        if (!isHoldExpired(row, OffsetDateTime.now()) && !Objects.equals(holdBy, row.getHoldBy())) {
            throw new IllegalStateException("Giữ chỗ này không thuộc về bạn.");
        }
        return new ShowtimeSeatView(null, showtimeId, seatId,
                null, null, null, SeatStatus.AVAILABLE.name(), null, null);
    }

    /** Internal API booking-service: chốt ghế BOOKED hàng loạt bằng 1 lượt chèn/cập nhật. */
    @Override
    @Transactional
    public SeatHoldResult bookSeats(Long showtimeId, List<Long> seatIds, String holdBy) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách ghế đặt không được để trống");
        }
        List<Long> requested = List.copyOf(new LinkedHashSet<>(seatIds));
        String sql = BOOK_SEATS_SQL.replace(
                "LITERAL_SEAT_IDS",
                requested.stream().map(String::valueOf).collect(Collectors.joining(","))); // id đã là Long, an toàn
        List<?> booked = em.createNativeQuery(sql)
                .setParameter("showtimeId", showtimeId)
                .setParameter("holdBy", holdBy)
                .getResultList();
        if (booked.size() >= requested.size()) {
            return SeatHoldResult.ok();
        }
        Set<Long> bookedIds = booked.stream().map(id -> ((Number) id).longValue()).collect(Collectors.toSet());
        List<Long> unavailable = requested.stream().filter(id -> !bookedIds.contains(id)).toList();
        return new SeatHoldResult(false, unavailable, "Một hoặc nhiều ghế đã được đặt / đang giữ");
    }

    /** Internal API booking-service: trả nhóm ghế về AVAILABLE trong 1 lượt cập nhật (bù trừ / hủy vé). */
    @Override
    @Transactional
    public void releaseSeatsInternal(Long showtimeId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }
        List<Long> distinct = List.copyOf(new LinkedHashSet<>(seatIds));
        String sql = RELEASE_SEATS_INTERNAL_SQL.replace(
                "LITERAL_SEAT_IDS",
                distinct.stream().map(String::valueOf).collect(Collectors.joining(",")));
        em.createNativeQuery(sql)
                .setParameter("showtimeId", showtimeId)
                .executeUpdate();
    }

    @Override
    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Showtime showtime = new Showtime();
        applyRequest(showtime, request, null);
        // Mô hình showtime_seats phát sinh theo tương tác: KHÔNG chèn sẵn ghế khi tạo suất.
        // Ghế để trống mặc định AVAILABLE; HOLDING/BOOKED chỉ xuất hiện khi có đặt vé.
        Showtime savedShowtime = showtimeRepository.save(showtime);
        return toResponse(savedShowtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtime(Long showtimeId, ShowtimeRequest request) {
        Showtime showtime = findShowtimeOrThrow(showtimeId);
        applyRequest(showtime, request, showtimeId);
        return toResponse(showtimeRepository.save(showtime));
    }

    @Override
    @Transactional
    public void deleteShowtime(Long showtimeId) {
        showtimeRepository.delete(findShowtimeOrThrow(showtimeId));
    }

    private Showtime findShowtimeOrThrow(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));
    }

    private String resolveStatus(ShowtimeSeat row, OffsetDateTime now) {
        if (row == null) {
            return SeatStatus.AVAILABLE.name();
        }
        if (row.getStatus() == SeatStatus.BOOKED) {
            return SeatStatus.BOOKED.name();
        }
        if (row.getStatus() == SeatStatus.AVAILABLE) {
            return SeatStatus.AVAILABLE.name();
        }
        return isHoldExpired(row, now) ? SeatStatus.AVAILABLE.name() : SeatStatus.HOLDING.name();
    }

    private boolean isHoldExpired(ShowtimeSeat row, OffsetDateTime now) {
        return row.getStatus() == SeatStatus.HOLDING
                && (row.getHoldUntil() == null || !row.getHoldUntil().isAfter(now));
    }

    /** 1 lệnh gọi venue cho mọi phòng của danh sách suất (tránh 1 Feign call / suất khi list lớn). */
    private Map<Long, RoomDto> loadRooms(List<Showtime> showtimes) {
        Set<Long> roomIds = showtimes.stream()
                .map(Showtime::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (roomIds.isEmpty()) {
            return Map.of();
        }
        return venueClient.getRoomsByIds(new ArrayList<>(roomIds)).stream()
                .collect(Collectors.toMap(RoomDto::roomId, Function.identity()));
    }

    private void applyRequest(Showtime showtime, ShowtimeRequest request, Long currentShowtimeId) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }

        // Kiểm tra trùng lịch chiếu trong phòng
        boolean isOverlapped = showtimeRepository.existsOverlappingShowtime(
                request.roomId(),
                request.startTime(),
                request.endTime(),
                currentShowtimeId
        );
        if (isOverlapped) {
            throw new IllegalArgumentException("Phòng chiếu đã có suất chiếu khác trong khoảng thời gian này");
        }

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new MovieNotFoundException(request.movieId()));

        // SOA: kiểm tra phòng tồn tại qua venue-service
        RoomDto room = venueClient.getRoom(request.roomId());
        if (room == null) {
            throw new NotFoundException("Không tìm thấy phòng chiếu với roomId = " + request.roomId());
        }

        showtime.setMovie(movie);
        showtime.setRoomId(request.roomId());
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(request.endTime());
        showtime.setBasePrice(request.basePrice());
        showtime.setStatus(request.status());
    }

    private ShowtimeResponse toResponse(Showtime showtime) {
        RoomDto room = venueClient.getRoom(showtime.getRoomId()); // SOA: data phòng qua venue-service
        return toResponse(showtime, room);
    }

    private ShowtimeResponse toResponse(Showtime showtime, RoomDto room) {
        return new ShowtimeResponse(
                showtime.getShowtimeId(),
                showtime.getMovie() != null ? showtime.getMovie().getMovieId() : null,
                showtime.getRoomId(),
                room != null ? room.name() : null,
                room != null ? room.roomType() : null,
                room != null ? room.cinemaId() : null,
                room != null ? room.cinemaName() : null,
                room != null ? room.cinemaAddress() : null,
                room != null ? room.cinemaCity() : null,
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice(),
                showtime.getStatus()
        );
    }
}
