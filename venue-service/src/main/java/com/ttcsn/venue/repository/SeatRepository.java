/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.venue.repository;

import com.ttcsn.venue.entity.Seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
/**
 *
 * @author quanghieu
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByRoom_RoomId(Long roomId);

    /** Ghế vật lý theo danh sách id, kèm phòng để dựng SeatDto. */
    @Query("SELECT s FROM Seat s JOIN FETCH s.room WHERE s.seatId IN :ids")
    List<Seat> findAllWithRoomByIds(@Param("ids") List<Long> ids);

    boolean existsByRoom_RoomIdAndSeatRowAndSeatNumber(Long roomId, String seatRow, Short seatNumber);
}