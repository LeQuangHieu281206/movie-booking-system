package com.ttcsn.booking.client;

import com.ttcsn.common.dto.ShowtimeView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * SOA Step 2 — Gọi movie-service (ĐỒNG BỘ, sync REST).
 * Lấy thông tin Phim / Suất chiếu cho nghiệp vụ đặt vé.
 */
@FeignClient(name = "movie-service", url = "${app.services.movie-url:http://localhost:8081}")
public interface MovieClient {

    /**.movie-service trả ShowtimeResponse — Jackson map sang ShowtimeView (fields tương thích). */
    @GetMapping("/api/getShowtimeById")
    ShowtimeView getShowtimeById(@RequestParam("id") Long id);
}
