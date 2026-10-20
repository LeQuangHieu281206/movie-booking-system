/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.venue.repository;

import com.ttcsn.venue.entity.Room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
/**
 *
 * @author quanghieu
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByCinema_CinemaId(Long cinemaId);

    boolean existsByCinema_CinemaIdAndName(Long cinemaId, String name);

    /** JOIN FETCH rạp trong 1 câu — tránh N+1 khi trả RoomDto cho movie-service (trễ qua pooler). */
    @Query("SELECT r FROM Room r JOIN FETCH r.cinema WHERE r.roomId IN :ids")
    List<Room> findAllWithCinemaByIds(@Param("ids") List<Long> ids);

    @Query("SELECT r FROM Room r JOIN FETCH r.cinema WHERE r.roomId = :roomId")
    Optional<Room> findWithCinemaById(@Param("roomId") Long roomId);
}
