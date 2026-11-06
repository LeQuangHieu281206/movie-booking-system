package com.ttcsn.booking.client;

import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.common.dto.SeatIdsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Gọi movie-service (suất chiếu + ghế) qua internal API.
 */
@FeignClient(name = "showtime-internal-client", url = "${app.services.movie-url:http://localhost:8081}")
public interface ShowtimeClient {

    @PostMapping("/internal/showtime-seats/hold")
    SeatHoldResult holdSeats(@RequestBody SeatIdsRequest request);

    @PostMapping("/internal/showtime-seats/release")
    SeatHoldResult releaseSeats(@RequestBody SeatIdsRequest request);
}
