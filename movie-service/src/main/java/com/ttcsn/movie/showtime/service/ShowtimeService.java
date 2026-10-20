/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.movie.showtime.service;

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

    /** Sơ đồ ghế của 1 suất chiếu (id + trạng thái) — dùng cho booking & frontend. */
    List<ShowtimeSeatView> getShowtimeSeats(Long showtimeId);

    ShowtimeResponse createShowtime(ShowtimeRequest request);

    ShowtimeResponse updateShowtime(Long showtimeId, ShowtimeRequest request);

    void deleteShowtime(Long showtimeId);
}