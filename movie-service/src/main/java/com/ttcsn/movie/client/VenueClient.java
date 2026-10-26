/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.client;

import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
@FeignClient(name = "venue-service", url = "${app.services.venue-url:http://localhost:8083}")
public interface VenueClient {

    @GetMapping("/internal/rooms/{roomId}")
    RoomDto getRoom(@PathVariable("roomId") Long roomId);

    @GetMapping("/internal/rooms/by-ids")
    List<RoomDto> getRoomsByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/internal/rooms/{roomId}/seats")
    List<SeatDto> getSeatsByRoom(@PathVariable("roomId") Long roomId);
}