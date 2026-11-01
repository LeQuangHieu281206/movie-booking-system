/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.dto;

import java.util.List;
import java.util.Map;
/**
 *
 * @author quanghieu
 */
public record SeatHoldResult(
        boolean success,
        List<Long> unavailableSeatIds,
        String message,
        Map<Long, Long> seatToShowtimeSeatId
) {
    public static SeatHoldResult ok(Map<Long, Long> seatToShowtimeSeatId) {
        return new SeatHoldResult(true, List.of(), "OK", seatToShowtimeSeatId);
    }

    public static SeatHoldResult ok() {
        return ok(Map.of());
    }
}
