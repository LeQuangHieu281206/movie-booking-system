package com.ttcsn.booking.service;

import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.entity.BookingDetail;
import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.config.RabbitMQConfig;
import com.ttcsn.booking.client.ShowtimeClient;
import com.ttcsn.booking.repository.BookingRepository;
import com.ttcsn.booking.repository.BookingDetailRepository;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.dto.SeatIdsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SOA Step 3 — Lắng nghe PaymentCompletedEvent từ RabbitMQ (async):
 * SUCCESS  -> Booking.status = PAID
 * FAILED   -> Booking.status = CANCELLED
 * (không tìm thấy booking -> WARN và bỏ qua — idempotent)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedListener {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final ShowtimeClient showtimeClient;

    @RabbitListener(queues = RabbitMQConfig.BOOKING_QUEUE)
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("[booking-service] Nhận PaymentCompletedEvent: bookingId={}, status={}, amount={}",
                event.bookingId(), event.status(), event.amount());

        bookingRepository.findById(event.bookingId()).ifPresentOrElse(booking -> {
            if (PaymentCompletedEvent.STATUS_SUCCESS.equalsIgnoreCase(event.status())) {
                booking.setStatus(BookingStatus.PAID);
                log.info("[booking-service] Booking #{} đã cập nhật sang PAID (amount={})",
                        event.bookingId(), event.amount());
            } else if (PaymentCompletedEvent.STATUS_FAILED.equalsIgnoreCase(event.status())) {
                if (booking.getStatus() == BookingStatus.PENDING) {
                    booking.setStatus(BookingStatus.CANCELLED);
                    log.warn("[booking-service] Booking #{} đã cập nhật sang CANCELLED (thanh toán thất bại)",
                            event.bookingId());
                    releaseSeatsOnFailure(booking);
                } else {
                    log.info("[booking-service] Booking #{} đang ở trạng thái {} — bỏ qua event FAILED (idempotent)",
                            event.bookingId(), booking.getStatus());
                }
            } else {
                log.warn("[booking-service] Status không hợp lệ: {}", event.status());
            }
            bookingRepository.save(booking);
        }, () -> log.warn("[booking-service] Không tìm thấy bookingId={} — bỏ qua event (idempotent)",
                event.bookingId()));
    }

    private void releaseSeatsOnFailure(Booking booking) {
        List<Long> seatIds = bookingDetailRepository.findByBookingId(booking.getBookingId()).stream()
                .map(BookingDetail::getSeatId)
                .toList();
        if (seatIds.isEmpty()) {
            return;
        }
        try {
            showtimeClient.releaseSeats(new SeatIdsRequest(booking.getShowtimeId(), seatIds, null));
            log.info("[booking-service] Đã trả ghế về AVAILABLE cho booking {} (bù trừ sau FAILED)",
                    booking.getBookingId());
        } catch (RuntimeException ex) {
            log.error("[booking-service] KHÔNG trả được ghế về AVAILABLE cho booking {}: {}",
                    booking.getBookingId(), ex.getMessage());
        }
    }
}
