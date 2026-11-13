/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.venue.controller;

import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatDto;
import com.ttcsn.venue.entity.Room;
import com.ttcsn.venue.entity.Seat;
import com.ttcsn.venue.repository.RoomRepository;
import com.ttcsn.venue.repository.SeatRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalVenueController {

    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;

    @GetMapping("/rooms/{roomId}")
    @Transactional(readOnly = true)
    public RoomDto getRoom(@PathVariable Long roomId) {
        return roomRepository.findWithCinemaById(roomId)
                .map(this::toRoomDto)
                .orElse(null);
    }

    @GetMapping("/rooms/{roomId}/seats")
    @Transactional(readOnly = true)
    public List<SeatDto> getSeatsByRoom(@PathVariable Long roomId) {
        return seatRepository.findByRoom_RoomId(roomId).stream()
                .map(this::toSeatDto)
                .toList();
    }

    /** Tra nhãn ghế hàng loạt cho booking-service (vé hiển thị vị trí ghế). */
    @PostMapping("/seats/by-ids")
    @Transactional(readOnly = true)
    public List<SeatDto> getSeatsByIds(@RequestBody List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }
        return seatRepository.findAllWithRoomByIds(seatIds).stream()
                .map(this::toSeatDto)
                .toList();
    }

    @GetMapping("/rooms/by-ids")
    @Transactional(readOnly = true)
    public List<RoomDto> getRoomsByIds(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return roomRepository.findAllWithCinemaByIds(ids).stream()
                .map(this::toRoomDto)
                .toList();
    }

    private SeatDto toSeatDto(Seat seat) {
        return new SeatDto(
                seat.getSeatId(),
                seat.getRoom().getRoomId(),
                seat.getSeatRow(),
                seat.getSeatNumber(),
                seat.getSeatType() != null ? seat.getSeatType().name() : null);
    }

    private RoomDto toRoomDto(Room room) {
        var cinema = room.getCinema();
        return new RoomDto(
                room.getRoomId(),
                room.getName(),
                room.getRoomType() != null ? room.getRoomType().getValue() : null,
                room.getTotalSeats() != null ? room.getTotalSeats().intValue() : null,
                cinema != null ? cinema.getCinemaId() : null,
                cinema != null ? cinema.getName() : null,
                cinema != null ? cinema.getAddress() : null,
                cinema != null ? cinema.getCity() : null
        );
    }
}