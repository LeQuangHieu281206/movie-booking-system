package com.ttcsn.booking.client;

import com.ttcsn.common.dto.RoomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * SOA Step 2 — Gọi venue-serviceKiểm tra rạp/phòng (không có trạng thái ghế-suất;
 * trạng thái ghế-theo-suất nằm ở movie-service — gọi qua ShowtimeClient).
 */
@FeignClient(name = "venue-service", url = "${app.services.venue-url:http://localhost:8083}")
public interface VenueClient {

    /** Kiểm tra tồn tại/đang hoạt động phòng chiếu trước khi hold ghế. */
    @GetMapping("/internal/rooms/{roomId}")
    RoomDto getRoom(@PathVariable("roomId") Long roomId);
}
