package com.ttcsn.payment.scheduling;

import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.entity.Payment;
import com.ttcsn.payment.entity.PaymentStatus;
import com.ttcsn.payment.repository.PaymentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Đóng các payment PENDING khi booking đã huỷ hoặc hết hạn.
 *
 * <p>Trước đây payment ở lại PENDING mãi, kể cả khi booking đã CANCELLED/EXPIRED.
 * Job này đối soát qua internal API của booking-service và chỉ cập nhật khi
 * payment vẫn còn PENDING, nên không ghi đè kết quả webhook đến đồng thời.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final BookingClient bookingClient;

    /** Bỏ qua payment vừa tạo để không tranh với webhook đang xử lý. */
    @Value("${app.payment.pending-reconcile-minutes:2}")
    private long pendingReconcileMinutes;

    @Scheduled(
            fixedDelayString = "${app.payment.reconcile-delay-ms:60000}",
            initialDelayString = "${app.payment.reconcile-initial-delay-ms:30000}"
    )
    public void closePendingPaymentsOfClosedBookings() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(pendingReconcileMinutes);
        List<Payment> stale = paymentRepository
                .findTop50ByStatusAndCreatedAtBeforeOrderByPaymentIdAsc(PaymentStatus.PENDING, cutoff);
        if (stale.isEmpty()) {
            return;
        }

        int closed = 0;
        for (Payment payment : stale) {
            PaymentStatus target = resolveTarget(payment.getBookingId());
            if (target == null) {
                continue;
            }
            int updated = paymentRepository.closeIfPending(
                    payment.getPaymentId(), target, OffsetDateTime.now());
            if (updated > 0) {
                closed++;
                log.info("[payment-service] Payment #{} (booking {}) PENDING -> {}",
                        payment.getPaymentId(), payment.getBookingId(), target);
            }
        }
        if (closed > 0) {
            log.info("[payment-service] Đã đóng {} payment PENDING của booking huỷ/hết hạn", closed);
        }
    }

    /** null = booking còn PENDING/PAID hoặc không đọc được, giữ nguyên payment. */
    private PaymentStatus resolveTarget(Long bookingId) {
        try {
            BookingPaymentView booking = bookingClient.getBooking(bookingId);
            String status = booking.status() == null ? "" : booking.status().toUpperCase(Locale.ROOT);
            return switch (status) {
                case "CANCELLED" -> PaymentStatus.CANCELLED;
                case "EXPIRED" -> PaymentStatus.EXPIRED;
                default -> null;
            };
        } catch (FeignException.NotFound e) {
            // Booking đã bị xoá: payment không còn cơ hội thành công.
            return PaymentStatus.CANCELLED;
        } catch (Exception e) {
            log.warn("[payment-service] Không đối soát được booking {}: {}", bookingId, e.getMessage());
            return null;
        }
    }
}
