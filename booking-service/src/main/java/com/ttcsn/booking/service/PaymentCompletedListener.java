package com.ttcsn.booking.service;

import com.ttcsn.booking.config.RabbitMQConfig;
import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.repository.BookingRepository;
import com.ttcsn.common.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * SOA Step 3 — PaymentCompletedEvent từ RabbitMQ (async):
 * SUCCESS + PENDING -> PAID
 * FAILED  + PENDING -> CANCELLED (trả ghế)
 * Trạng thái khác / booking không tồn tại -> bỏ qua (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedListener {

    private final BookingRepository bookingRepository;
    private final BookingSeatReleaser seatReleaser;

    @RabbitListener(queues = RabbitMQConfig.BOOKING_QUEUE)
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        bookingRepository.findById(event.bookingId()).ifPresentOrElse(booking -> {
            if (PaymentCompletedEvent.STATUS_SUCCESS.equalsIgnoreCase(event.status())) {
                markPaid(booking);
                return;
            }
            if (PaymentCompletedEvent.STATUS_FAILED.equalsIgnoreCase(event.status())) {
                cancelUnpaid(booking);
                return;
            }
            log.warn("[booking-service] Status không hợp lệ: {}", event.status());
        }, () -> log.warn("[booking-service] Không tìm thấy bookingId={} — bỏ qua event (idempotent)",
                event.bookingId()));
    }

    private void markPaid(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            log.info("[booking-service] Booking #{} đang ở trạng thái {} — bỏ qua event SUCCESS (idempotent)",
                    booking.getBookingId(), booking.getStatus());
            return;
        }
        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);
        log.info("[booking-service] Booking #{} đã cập nhật sang PAID", booking.getBookingId());
    }

    private void cancelUnpaid(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            log.info("[booking-service] Booking #{} đang ở trạng thái {} — bỏ qua event FAILED (idempotent)",
                    booking.getBookingId(), booking.getStatus());
            return;
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.warn("[booking-service] Booking #{} đã cập nhật sang CANCELLED (thanh toán thất bại)",
                booking.getBookingId());
        seatReleaser.release(booking);
    }
}
