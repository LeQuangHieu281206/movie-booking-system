package com.ttcsn.movie.showtime.service.impl;

import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatDto;
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
import com.ttcsn.movie.showtime.exception.ShowtimeNotFoundException;
import com.ttcsn.movie.showtime.repository.ShowtimeRepository;
import com.ttcsn.movie.showtime.repository.ShowtimeSeatRepository;
import com.ttcsn.movie.showtime.service.ShowtimeService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 *
 * @author quanghieu
 */
@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final VenueClient venueClient; // SOA: thay cho Room/Seat repository của monolith

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getAllShowtimes() {
        return showtimeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByMovieId(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new MovieNotFoundException(movieId);
        }
        return showtimeRepository.findByMovie_MovieId(movieId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getShowtimeById(Long showtimeId) {
        return toResponse(findShowtimeOrThrow(showtimeId));
    }

    @Override
    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Showtime showtime = new Showtime();
        applyRequest(showtime, request, null);
        Showtime savedShowtime = showtimeRepository.save(showtime);

        // Tự động sinh danh sách ghế cho suất chiếu này (showtime_seats)
        // SOA: ghế vật lý lấy từ venue-service qua Feign thay vì SeatRepository
        List<SeatDto> roomSeats = venueClient.getSeatsByRoom(savedShowtime.getRoomId());
        if (!roomSeats.isEmpty()) {
            List<ShowtimeSeat> showtimeSeats = roomSeats.stream()
                    .map(seat -> ShowtimeSeat.builder()
                            .showtime(savedShowtime)
                            .seatId(seat.seatId())
                            .status(SeatStatus.AVAILABLE)
                            .updatedAt(OffsetDateTime.now())
                            .build())
                    .toList();
            showtimeSeatRepository.saveAll(showtimeSeats);
        }

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

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeSeatView> getShowtimeSeats(Long showtimeId) {
        // xác thực suất tồn tại (ShowtimeNotFoundException nếu không)
        findShowtimeOrThrow(showtimeId);
        return showtimeSeatRepository.findByShowtime_ShowtimeId(showtimeId).stream()
                .map(seat -> new ShowtimeSeatView(
                        seat.getShowtimeSeatId(),
                        seat.getShowtime().getShowtimeId(),
                        seat.getSeatId(),
                        seat.getStatus().name()))
                .toList();
    }

    private Showtime findShowtimeOrThrow(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeNotFoundException(showtimeId));
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
