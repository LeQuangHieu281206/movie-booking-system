/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.movie.showtime.repository;

import com.ttcsn.movie.showtime.entity.ShowtimeSeat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
/**
 *
 * @author quanghieu
 */
@Repository
public interface ShowtimeSeatRepository extends JpaRepository<ShowtimeSeat, Long> {
    List<ShowtimeSeat> findByShowtime_ShowtimeId(Long showtimeId);
    Optional<ShowtimeSeat> findByShowtime_ShowtimeIdAndSeatId(Long showtimeId, Long seatId);
    List<ShowtimeSeat> findByShowtime_ShowtimeIdAndSeatIdIn(Long showtimeId, Collection<Long> seatIds);
}