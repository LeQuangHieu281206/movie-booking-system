package com.ttcsn.notification.listener;

import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.notification.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * SOA Step 3 — notification-service lắng nghe PaymentCompletedEvent:
 * thanh toán SUCCESS -> hiện thông báo "Đã gửi Email vé cho khách hàng".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedListener {

    // Tạm log; sau nối JavaMailSender / SendGrid.
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (!PaymentCompletedEvent.STATUS_SUCCESS.equalsIgnoreCase(event.status())) {
            log.info("[notification-service] Bỏ qua event FAILED (booking #{}), không gửi email",
                    event.bookingId());
            return;
        }
        // TODO sau: gọi user-service lấy email khách hàng rồi send mail thật
        log.info("[notification-service] Đã gửi Email vé cho khách hàng (bookingId={}, userId={}, tổng tiền={}, paymentTime={})",
                event.bookingId(), event.userId(), event.amount(), event.paymentTime());
    }
}
