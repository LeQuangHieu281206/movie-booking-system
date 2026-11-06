/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.venue.service.impl;

import com.ttcsn.venue.dto.request.SeatRequest;
import com.ttcsn.venue.dto.response.SeatResponse;
import com.ttcsn.venue.entity.Room;
import com.ttcsn.venue.entity.Seat;
import com.ttcsn.venue.exception.RoomNotFoundException;
import com.ttcsn.venue.exception.SeatNotFoundException;
import com.ttcsn.venue.repository.RoomRepository;
import com.ttcsn.venue.repository.SeatRepository;
import com.ttcsn.venue.service.SeatService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 *
 * @author quanghieu
 */
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByRoomId(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RoomNotFoundException(roomId);
        }
        return seatRepository.findByRoom_RoomId(roomId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getSeatById(Long seatId) {
        return toResponse(findSeatOrThrow(seatId));
    }

    @Override
    @Transactional
    public SeatResponse createSeat(SeatRequest request) {
        ensurePositionFree(request.roomId(), request.seatRow(), request.seatNumber(), null);
        Seat seat = new Seat();
        applyRequest(seat, request);
        return toResponse(seatRepository.save(seat));
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(Long seatId, SeatRequest request) {
        Seat seat = findSeatOrThrow(seatId);
        boolean moved = !java.util.Objects.equals(seat.getRoom().getRoomId(), request.roomId())
                || !java.util.Objects.equals(seat.getSeatRow(), request.seatRow())
                || !java.util.Objects.equals(seat.getSeatNumber(), request.seatNumber());
        if (moved) {
            ensurePositionFree(request.roomId(), request.seatRow(), request.seatNumber(), seatId);
        }
        applyRequest(seat, request);
        return toResponse(seatRepository.save(seat));
    }

    /**
     * Một ghế chỉ có một vị trí trong mỗi phòng. Bỏ qua chính bản ghi đang sửa
     * để đổi loại ghế không bị coi là trùng.
     */
    private void ensurePositionFree(Long roomId, String seatRow, Short seatNumber, Long excludingSeatId) {
        if (!seatRepository.existsByRoom_RoomIdAndSeatRowAndSeatNumber(roomId, seatRow, seatNumber)) {
            return;
        }
        if (excludingSeatId != null) {
            boolean sameSeat = seatRepository.findById(excludingSeatId)
                    .map(seat -> java.util.Objects.equals(seat.getRoom().getRoomId(), roomId)
                            && java.util.Objects.equals(seat.getSeatRow(), seatRow)
                            && java.util.Objects.equals(seat.getSeatNumber(), seatNumber))
                    .orElse(false);
            if (sameSeat) {
                return;
            }
        }
        throw new IllegalArgumentException("Vị trí ghế (Hàng " + seatRow + ", Số " + seatNumber
                + ") đã tồn tại trong phòng này");
    }

    @Override
    @Transactional
    public void deleteSeat(Long seatId) {
        seatRepository.delete(findSeatOrThrow(seatId));
    }

    private Seat findSeatOrThrow(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatNotFoundException(seatId));
    }

    private void applyRequest(Seat seat, SeatRequest request) {
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new RoomNotFoundException(request.roomId()));

        seat.setRoom(room);
        seat.setSeatRow(request.seatRow());
        seat.setSeatNumber(request.seatNumber());
        seat.setSeatType(request.seatType());
    }

    private SeatResponse toResponse(Seat seat) {
        return new SeatResponse(
                seat.getSeatId(),
                seat.getSeatRow(),
                seat.getSeatNumber(),
                seat.getSeatType()
        );
    }
}