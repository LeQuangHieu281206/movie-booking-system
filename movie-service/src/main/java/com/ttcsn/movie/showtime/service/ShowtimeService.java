/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.movie.showtime.service;

import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.movie.showtime.dto.request.ShowtimeRequest;
import com.ttcsn.movie.showtime.dto.response.ShowtimeResponse;
import com.ttcsn.movie.showtime.dto.response.ShowtimeSeatView;

import java.util.List;
/**
 *
 * @author quanghieu
 */
public interface ShowtimeService {

    List<ShowtimeResponse> getAllShowtimes();

    List<ShowtimeResponse> getShowtimesByMovieId(Long movieId);

    ShowtimeResponse getShowtimeById(Long showtimeId);

    List<ShowtimeSeatView> getShowtimeSeats(Long showtimeId);

    ShowtimeSeatView holdShowtimeSeat(Long showtimeId, Long seatId, Long userId);

    ShowtimeSeatView releaseShowtimeSeat(Long showtimeId, Long seatId, Long userId, boolean admin);

    SeatHoldResult bookSeats(Long showtimeId, List<Long> seatIds, String holdBy);

    void releaseSeatsInternal(Long showtimeId, List<Long> seatIds);

    ShowtimeResponse createShowtime(ShowtimeRequest request);

    ShowtimeResponse updateShowtime(Long showtimeId, ShowtimeRequest request);

    void deleteShowtime(Long showtimeId);
}