package com.ttcsn.payment.service;

import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingClient bookingClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService service;

    @Test
    void paysOnlyOwnPendingBookingUsingServerAmount() {
        when(bookingClient.getBooking(1L))
                .thenReturn(new BookingPaymentView(1L, 7L, new BigDecimal("100000"), "PENDING"));

        PaymentCompletedEvent event = service.processPayment(1L, 7L, false);

        assertThat(event.bookingId()).isEqualTo(1L);
        assertThat(event.userId()).isEqualTo(7L);
        assertThat(event.amount()).isEqualByComparingTo("100000");
        assertThat(event.status()).isEqualTo(PaymentCompletedEvent.STATUS_SUCCESS);
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.EXCHANGE, Topics.PAYMENT_COMPLETED, event);
    }

    @Test
    void rejectsBookingOfAnotherUser() {
        when(bookingClient.getBooking(1L))
                .thenReturn(new BookingPaymentView(1L, 8L, new BigDecimal("100000"), "PENDING"));

        assertThrows(NotFoundException.class, () -> service.processPayment(1L, 7L, false));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void rejectsBookingThatIsNotPending() {
        when(bookingClient.getBooking(1L))
                .thenReturn(new BookingPaymentView(1L, 7L, new BigDecimal("100000"), "PAID"));

        assertThrows(ConflictException.class, () -> service.processPayment(1L, 7L, false));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void rejectsInvalidServerAmount() {
        when(bookingClient.getBooking(1L))
                .thenReturn(new BookingPaymentView(1L, 7L, BigDecimal.ZERO, "PENDING"));

        assertThrows(ConflictException.class, () -> service.processPayment(1L, 7L, false));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void duplicatePaymentForSameBookingIsIdempotent() {
        when(bookingClient.getBooking(1L))
                .thenReturn(new BookingPaymentView(1L, 7L, new BigDecimal("100000"), "PENDING"));

        PaymentCompletedEvent first = service.processPayment(1L, 7L, false);
        PaymentCompletedEvent second = service.processPayment(1L, 7L, false);

        assertThat(second.paymentId()).isEqualTo(first.paymentId());
        verify(bookingClient, times(1)).getBooking(1L);
        verify(rabbitTemplate, times(1)).convertAndSend(
                RabbitMQConfig.EXCHANGE, Topics.PAYMENT_COMPLETED, first);
    }

    @Test
    void duplicateFromAnotherUserIsNotLeaked() {
        when(bookingClient.getBooking(1L))
                .thenReturn(new BookingPaymentView(1L, 7L, new BigDecimal("100000"), "PENDING"));
        service.processPayment(1L, 7L, false);

        assertThrows(NotFoundException.class, () -> service.processPayment(1L, 8L, false));
        verify(bookingClient, times(1)).getBooking(1L);
        verify(rabbitTemplate, times(1)).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EXCHANGE),
                org.mockito.ArgumentMatchers.eq(Topics.PAYMENT_COMPLETED),
                org.mockito.ArgumentMatchers.any(PaymentCompletedEvent.class));
    }
}
