/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.notification.listener;

import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.notification.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
/**
 *
 * @author quanghieu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedListener {

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (!PaymentCompletedEvent.STATUS_SUCCESS.equalsIgnoreCase(event.status())) {
            log.info("[notification-service] Bỏ qua event FAILED (booking #{}), không gửi email",
                    event.bookingId());
            return;
        }
        log.info("[notification-service] Đã gửi Email vé cho khách hàng (bookingId={}, userId={}, tổng tiền={}, paymentTime={})",
                event.bookingId(), event.userId(), event.amount(), event.paymentTime());
    }
}