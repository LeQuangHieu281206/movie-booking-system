package com.ttcsn.common.event;

/**
 * Tên routing-key dùng chung cho RabbitMQ.
 * Chỉ khai báo event đang thực sự được publish/consume.
 */
public final class Topics {

    private Topics() {
    }

    /** payment-service publish; booking-service và notification-service consume. */
    public static final String PAYMENT_COMPLETED = "payment.completed";
}
