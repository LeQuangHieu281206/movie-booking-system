/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.dto.request;

import jakarta.validation.constraints.NotNull;
/**
 *
 * @author quanghieu
 */
public record ShowtimeSeatInteractionRequest(
        @NotNull(message = "showtimeId không được để trống")
        Long showtimeId,

        @NotNull(message = "seatId không được để trống")
        Long seatId
) {
}