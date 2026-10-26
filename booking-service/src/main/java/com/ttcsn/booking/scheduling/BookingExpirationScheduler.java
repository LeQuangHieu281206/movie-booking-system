package com.ttcsn.booking.scheduling;

import com.ttcsn.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job định kỳ hết hạn booking PENDING quá hạn chờ thanh toán (mặc định 15 phút,
 * cấu hình {@code app.booking.pending-expiry-minutes}) và nhả ghế cho khách khác đặt.
 *
 * <p>Không có job này, booking PENDING bị bỏ quên sẽ giữ ghế BOOKED vĩnh viễn:
 * không có payment event nào đến để PaymentCompletedListener nhả ghế, và EXPIRED
 * trước đây chỉ do ADMIN gọi API thủ công.
 *
 * <p>fixedDelay (không phải cron): nếu lượt chạy trước chưa xong thì lượt sau dời lại,
 * tránh chồng lượt khi DB chậm.
 *
 * @author quanghieu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingService bookingService;

    @Scheduled(
            fixedDelayString = "${app.booking.expiry-check-interval-ms:60000}",
            initialDelayString = "${app.booking.expiry-initial-delay-ms:60000}")
    public void expireStalePendingBookings() {
        int expired = bookingService.expireStalePendingBookings();
        if (expired > 0) {
            log.info("[booking-service] Scheduler đã hết hạn {} booking PENDING quá hạn", expired);
        }
    }
}
