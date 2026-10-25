package com.ttcsn.booking.dto.request;

import com.ttcsn.booking.entity.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * userId lấy từ header X-User-Id do gateway gắn; totalAmount do server tính
 * từ giá suất chiếu và số ghế. Client không được gửi hai giá trị này.
 */
public record BookingCreateRequest(
        @NotNull(message = "showtimeId không được để trống")
        Long showtimeId,

        @NotEmpty(message = "Danh sách ghế đặt không được để trống")
        List<Long> seatIds,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod paymentMethod
) {
}
