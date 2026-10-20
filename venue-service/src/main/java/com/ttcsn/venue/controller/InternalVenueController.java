package com.ttcsn.venue.controller;

import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatDto;
import com.ttcsn.venue.entity.Room;
import com.ttcsn.venue.repository.RoomRepository;
import com.ttcsn.venue.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal API — movie-service gọi sang khi tạo showtime / render showtime detail.
 * Trả DTO dạng đủ nhẹ (RoomDto/SeatDto) — anti-corruption layer của venue.
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
                .orElse(null); // null -> phía gọi tự quyết định NotFoundException
    }

    @GetMapping("/rooms/{roomId}/seats")
    @Transactional(readOnly = true)
    public List<SeatDto> getSeatsByRoom(@PathVariable Long roomId) {
        return seatRepository.findByRoom_RoomId(roomId).stream()
                .map(seat -> new SeatDto(
                        seat.getSeatId(),
                        seat.getRoom().getRoomId(),
                        seat.getSeatRow(),
                        seat.getSeatNumber(),
                        seat.getSeatType() != null ? seat.getSeatType().name() : null))
                .toList();
    }

    /** Lấy nhiều phòng trong 1 lần gọi + 1 câu JOIN FETCH rạp — tránh 1 Feign call / suất (list 1.000+ suất). */
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

    private RoomDto toRoomDto(Room room) {
        return new RoomDto(
                room.getRoomId(),
                room.getName(),
                room.getRoomType() != null ? room.getRoomType().getValue() : null,
                room.getTotalSeats() != null ? room.getTotalSeats().intValue() : null,
                room.getCinema() != null ? room.getCinema().getCinemaId() : null,
                room.getCinema() != null ? room.getCinema().getName() : null,
                room.getCinema() != null ? room.getCinema().getAddress() : null,
                room.getCinema() != null ? room.getCinema().getCity() : null
        );
    }
}
