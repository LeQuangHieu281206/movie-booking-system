/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.event;

import java.util.List;
import java.util.UUID;
/**
 *
 * @author quanghieu
 */
public record SeatHoldRequestedEvent(
        UUID sagaId,
        Long showtimeId,
        List<Long> showtimeSeatIds,
        long ttlSeconds
) {}