/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.repository;

import com.ttcsn.movie.showtime.entity.Showtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
/**
 *
 * @author quanghieu
 */
@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovie_MovieId(Long movieId);

    @Query("SELECT s FROM Showtime s JOIN FETCH s.movie WHERE s.movie.movieId = :movieId")
    List<Showtime> findAllWithMovieByMovieId(@Param("movieId") Long movieId);

    List<Showtime> findByRoomId(Long roomId);

    @Query("SELECT COUNT(s) > 0 FROM Showtime s WHERE s.roomId = :roomId " +
           "AND (:showtimeId IS NULL OR s.showtimeId <> :showtimeId) " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    boolean existsOverlappingShowtime(
            @Param("roomId") Long roomId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("showtimeId") Long showtimeId);
}