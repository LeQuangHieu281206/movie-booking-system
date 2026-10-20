package com.ttcsn.common.event;

/**
 * SOA Step 0 — Chốt tên các event cho Saga Choreography (dùng từ Step 4-5).
 * Không đổi tên sau khi RabbitMQ đã tạo queue/bind. Lưu đây làm nguồn sự thật duy nhất.
 */
public final class Topics {
    private Topics() {}

    // showtime-service (Seat Inventory)
    public static final String SEAT_HELD       = "seat.held";        // ghế được HOLD cho 1 saga
    public static final String SEAT_RELEASED   = "seat.released";    // ghế trả về AVAILABLE (bù / hết TTL)
    public static final String SEAT_CONFIRMED  = "seat.confirmed";   // thanh toán OK -> ghế chốt

    // booking-service
    public static final String BOOKING_CREATED    = "booking.created";
    public static final String BOOKING_CONFIRMED  = "booking.confirmed";
    public static final String BOOKING_CANCELLED  = "booking.cancelled";

    // payment-service (Step 6)
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED    = "payment.failed";

    // user-service
    public static final String USER_REGISTERED = "user.registered";
}
