/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.dto;
/**
 *
 * @author quanghieu
 */
public record RoomDto(
        Long roomId,
        String name,
        String roomType,
        Integer totalSeats,
        Long cinemaId,
        String cinemaName,
        String cinemaAddress,
        String cinemaCity
) {}