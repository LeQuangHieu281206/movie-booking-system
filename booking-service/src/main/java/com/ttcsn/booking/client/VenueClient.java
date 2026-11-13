package com.ttcsn.booking.client;

import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * SOA Step 2 — Gọi venue-serviceKiểm tra rạp/phòng (không có trạng thái ghế-suất;
 * trạng thái ghế-theo-suất nằm ở movie-service — gọi qua ShowtimeClient).
 */
@FeignClient(name = "venue-service")
public interface VenueClient {

    /** Kiểm tra tồn tại/đang hoạt động phòng chiếu trước khi hold ghế. */
    @GetMapping("/internal/rooms/{roomId}")
    RoomDto getRoom(@PathVariable("roomId") Long roomId);

    /** Nhãn ghế (hàng + số) cho danh sách seatId, dùng khi trả vé kèm vị trí ghế. */
    @PostMapping("/internal/seats/by-ids")
    List<SeatDto> getSeatsByIds(@RequestBody List<Long> seatIds);
}
