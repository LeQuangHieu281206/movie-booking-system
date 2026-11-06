/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.service.impl;

import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatDto;
import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.common.dto.SeatIds;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.movie.client.VenueClient;
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
    private static final int SEATS_CACHE_MAX_ROOMS = 200;

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

    private static final String RELEASE_SEAT_SQL = """
            WITH deleted AS (
                DELETE FROM showtime_seats
                WHERE showtime_id = :showtimeId AND seat_id = :seatId
                  AND status = 'HOLDING'
                  AND (:isAdmin = true OR hold_by = :holdBy OR hold_until <= now())
                RETURNING showtime_seat_id
            )
            SELECT count(*) FROM deleted
            """;

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

    /** Bù trừ: trả một nhóm ghế về AVAILABLE trong 1 lượt.
     *  Giữ dòng để audit lịch sử giữ/đặt ghế; AVAILABLE ≈ "không có dòng". */
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
        if (seatsByRoomCache.size() >= SEATS_CACHE_MAX_ROOMS) {
            seatsByRoomCache.clear();
        }
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
        Showtime showtime = findShowtimeOrThrow(showtimeId);
        return toResponse(showtime, venueClient.getRoom(showtime.getRoomId()));
    }

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
                            hasRecord ? row.getHoldUntil() : null);
                })
                .sorted(Comparator
                        .comparing((ShowtimeSeatView v) -> v.seatRow() == null ? "" : v.seatRow())
                        .thenComparing(v -> v.seatNumber() == null ? Short.MAX_VALUE : v.seatNumber()))
                .toList();
    }

    @Override
    @Transactional
    public ShowtimeSeatView holdShowtimeSeat(Long showtimeId, Long seatId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Thiếu X-User-Id");
        }
        String holdBy = String.valueOf(userId);
        OffsetDateTime holdUntil = OffsetDateTime.now().plus(HOLD_DURATION);
        List<?> rows = em.createNativeQuery(HOLD_SEAT_SQL)
                .setParameter("showtimeId", showtimeId)
                .setParameter("seatId", seatId)
                .setParameter("holdUntil", holdUntil)
                .setParameter("holdBy", holdBy)
                .getResultList();

        if (!rows.isEmpty()) { 
            return new ShowtimeSeatView(((Number) rows.get(0)).longValue(), showtimeId, seatId,
                    null, null, null, SeatStatus.HOLDING.name(), holdUntil);
        }
        throwHoldRejection(showtimeId, seatId, holdBy);
        throw new ConflictException("Ghế vừa được giữ bởi phiên khác. Chọn ghế khác.");
    }

    private void throwHoldRejection(Long showtimeId, Long seatId, String holdBy) {
        Showtime showtime = showtimeRepository.findById(showtimeId).orElse(null);
        if (showtime == null) {
            throw new ConflictException("Suất chiếu không tồn tại.");
        }
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new ConflictException("Suất chiếu không còn nhận đặt vé (trạng thái " + showtime.getStatus() + ").");
        }
        if (!showtime.getStartTime().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("Suất chiếu đã bắt đầu, không thể giữ ghế.");
        }
        ShowtimeSeat row = showtimeSeatRepository.findByShowtime_ShowtimeIdAndSeatId(showtimeId, seatId).orElse(null);
        if (row == null) {
            throw new ConflictException("Ghế không thuộc sơ đồ của phòng chiếu này.");
        }
        if (row.getStatus() == SeatStatus.BOOKED) {
            throw new ConflictException("Ghế này đã được đặt thành công.");
        }
        ensureNotHeldByOther(row, holdBy);
    }

    /** Một chỗ duy nhất quyết định "ghế đang bị người khác giữ" cho cả hold và release. */
    private void ensureNotHeldByOther(ShowtimeSeat row, String holdBy) {
        if (row.getStatus() == SeatStatus.HOLDING
                && !isHoldExpired(row, OffsetDateTime.now())
                && !Objects.equals(holdBy, row.getHoldBy())) {
            throw new ConflictException("Ghế đang được khách khác giữ. Chọn ghế khác.");
        }
    }

    @Override
    @Transactional
    public ShowtimeSeatView releaseShowtimeSeat(Long showtimeId, Long seatId, Long userId, boolean admin) {
        if (userId == null) {
            throw new IllegalArgumentException("Thiếu X-User-Id");
        }
        String holdBy = String.valueOf(userId);
        if (!admin) {
            ShowtimeSeat current = showtimeSeatRepository
                    .findByShowtime_ShowtimeIdAndSeatId(showtimeId, seatId).orElse(null);
            if (current != null) {
                ensureNotHeldByOther(current, holdBy);
            }
        }

        Object deleted = em.createNativeQuery(RELEASE_SEAT_SQL)
                .setParameter("showtimeId", showtimeId)
                .setParameter("seatId", seatId)
                .setParameter("holdBy", holdBy)
                .setParameter("isAdmin", admin)
                .getSingleResult();
        if (((Number) deleted).longValue() > 0) {
            return new ShowtimeSeatView(null, showtimeId, seatId,
                    null, null, null, SeatStatus.AVAILABLE.name(), null);
        }
        ShowtimeSeat row = showtimeSeatRepository.findByShowtime_ShowtimeIdAndSeatId(showtimeId, seatId).orElse(null);
        if (row == null || row.getStatus() == SeatStatus.AVAILABLE) {
            return new ShowtimeSeatView(null, showtimeId, seatId,
                    null, null, null, SeatStatus.AVAILABLE.name(), null);
        }
        if (row.getStatus() == SeatStatus.BOOKED) {
            throw new ConflictException("Ghế đã được đặt thành công, không thể bỏ giữ. Hủy vé để trả ghế.");
        }
        ensureNotHeldByOther(row, holdBy);
        return new ShowtimeSeatView(null, showtimeId, seatId,
                null, null, null, SeatStatus.AVAILABLE.name(), null);
    }

    @Override
    @Transactional
    public SeatHoldResult bookSeats(Long showtimeId, List<Long> seatIds, String holdBy) {
        List<Long> requested = SeatIds.normalize(seatIds);
        String sql = BOOK_SEATS_SQL.replace(
                "LITERAL_SEAT_IDS",
                requested.stream().map(String::valueOf).collect(Collectors.joining(","))); // id đã là Long, an toàn
        List<?> booked = em.createNativeQuery(sql)
                .setParameter("showtimeId", showtimeId)
                .setParameter("holdBy", holdBy)
                .getResultList();
        if (booked.size() >= requested.size()) {
            Map<Long, Long> seatToShowtimeSeatId = showtimeSeatRepository
                    .findByShowtime_ShowtimeIdAndSeatIdIn(showtimeId, requested)
                    .stream()
                    .collect(Collectors.toMap(ShowtimeSeat::getSeatId, ShowtimeSeat::getShowtimeSeatId));
            return SeatHoldResult.ok(seatToShowtimeSeatId);
        }
        Set<Long> bookedIds = booked.stream().map(id -> ((Number) id).longValue()).collect(Collectors.toSet());
        List<Long> unavailable = requested.stream().filter(id -> !bookedIds.contains(id)).toList();
        return new SeatHoldResult(false, unavailable, "Một hoặc nhiều ghế đã được đặt / đang giữ", Map.of());
    }

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
        RoomDto room = applyRequest(showtime, request, null);
        Showtime savedShowtime = showtimeRepository.save(showtime);
        return toResponse(savedShowtime, room);
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtime(Long showtimeId, ShowtimeRequest request) {
        Showtime showtime = findShowtimeOrThrow(showtimeId);
        RoomDto room = applyRequest(showtime, request, showtimeId);
        return toResponse(showtimeRepository.save(showtime), room);
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

    private RoomDto applyRequest(Showtime showtime, ShowtimeRequest request, Long currentShowtimeId) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("endTime phải sau startTime");
        }

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
        return room;
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
