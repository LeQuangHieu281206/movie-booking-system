package com.ttcsn.movie.client;

import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * SOA: movie-service gọi venue-service lấy thông tin phòng/ghế.
 * URL cấu hình tại app.services.venue-url (mặc định http://localhost:8083).
 */
@FeignClient(name = "venue-service", url = "${app.services.venue-url:http://localhost:8083}")
public interface VenueClient {

    @GetMapping("/internal/rooms/{roomId}")
    RoomDto getRoom(@PathVariable("roomId") Long roomId);

    @GetMapping("/internal/rooms/{roomId}/seats")
    List<SeatDto> getSeatsByRoom(@PathVariable("roomId") Long roomId);
}
