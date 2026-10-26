/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.dto;

import java.util.List;
/**
 *
 * @author quanghieu
 */
public record SeatHoldResult(
        boolean success,
        List<Long> unavailableSeatIds,
        String message
) {
    public static SeatHoldResult ok() {
        return new SeatHoldResult(true, List.of(), "OK");
    }
}