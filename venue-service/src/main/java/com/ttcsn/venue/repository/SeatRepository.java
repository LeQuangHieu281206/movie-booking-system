/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.venue.repository;

import com.ttcsn.venue.entity.Seat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
/**
 *
 * @author quanghieu
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByRoom_RoomId(Long roomId);

    boolean existsByRoom_RoomIdAndSeatRowAndSeatNumber(Long roomId, String seatRow, Short seatNumber);
}