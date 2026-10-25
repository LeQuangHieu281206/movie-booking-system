package com.ttcsn.payment.client;

import com.ttcsn.common.dto.BookingPaymentView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Internal API booking-service: đọc booking để xác thực trước khi thanh toán. */
@FeignClient(name = "booking-service", url = "${app.services.booking-url:http://127.0.0.1:8082}")
public interface BookingClient {

    @GetMapping("/internal/bookings/{bookingId}")
    BookingPaymentView getBooking(@PathVariable("bookingId") Long bookingId);
}
