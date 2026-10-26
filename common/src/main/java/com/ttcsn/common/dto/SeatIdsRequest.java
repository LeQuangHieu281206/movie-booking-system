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
public record SeatIdsRequest(
        Long showtimeId,
        List<Long> seatIds,
        String holdBy
) {}