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

    /** Sơ đồ ghế của 1 suất chiếu (ghế phòng + bản ghi giữ/đặt theo tương tác). */
    List<ShowtimeSeatView> getShowtimeSeats(Long showtimeId);

    /** Giữ chỗ tạm 10 phút; holdBy do server gán từ userId. */
    ShowtimeSeatView holdShowtimeSeat(Long showtimeId, Long seatId, Long userId);

    /** Bỏ giữ chỗ: chỉ người giữ hoặc ADMIN. */
    ShowtimeSeatView releaseShowtimeSeat(Long showtimeId, Long seatId, Long userId, boolean admin);

    /** Internal: booking-service chốt ghế BOOKED khi tạo vé. */
    SeatHoldResult bookSeats(Long showtimeId, List<Long> seatIds, String holdBy);

    /** Internal: booking-service trả ghế về AVAILABLE (hủy vé / bù trừ). */
    void releaseSeatsInternal(Long showtimeId, List<Long> seatIds);

    ShowtimeResponse createShowtime(ShowtimeRequest request);

    ShowtimeResponse updateShowtime(Long showtimeId, ShowtimeRequest request);

    void deleteShowtime(Long showtimeId);
}
