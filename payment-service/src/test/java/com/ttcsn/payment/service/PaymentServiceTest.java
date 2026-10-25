package com.ttcsn.payment.service;

import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.config.RabbitMQConfig;
import com.ttcsn.payment.dto.request.SePayWebhookDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private static BookingPaymentView booking(Long bookingId, Long userId, String amount, String status) {
        return new BookingPaymentView(bookingId, userId, amount == null ? null : new BigDecimal(amount), status);
    }

    // ---------- createSePayPayment ----------

    @Test
    void generatesQrForOwnPendingBookingUsingServerAmount() {
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PENDING"));

        String qrUrl = service.createSePayPayment(1L, 7L);

        assertThat(qrUrl).contains("amount=100000").contains("des=DH1");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void rejectsBookingOfAnotherUser() {
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 8L, "100000", "PENDING"));

        assertThrows(NotFoundException.class, () -> service.createSePayPayment(1L, 7L));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void rejectsBookingThatIsNotPending() {
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PAID"));

        assertThrows(ConflictException.class, () -> service.createSePayPayment(1L, 7L));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void rejectsInvalidServerAmount() {
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "0", "PENDING"));

        assertThrows(ConflictException.class, () -> service.createSePayPayment(1L, 7L));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void rejectsMissingBookingId() {
        assertThrows(IllegalArgumentException.class, () -> service.createSePayPayment(null, 7L));
        verifyNoInteractions(bookingClient, rabbitTemplate);
    }

    // ---------- processSePayWebhook ----------

    @Test
    void webhookPublishesSuccessEventForIncomingTransfer() {
        SePayWebhookDto dto = new SePayWebhookDto();
        dto.setTransferType("in");
        dto.setContent("DH1 nguoi chuyen khoan");
        dto.setTransferAmount(new BigDecimal("100000"));

        service.processSePayWebhook(dto);

        ArgumentCaptor<PaymentCompletedEvent> captor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(Topics.PAYMENT_COMPLETED), captor.capture());
        PaymentCompletedEvent event = captor.getValue();
        assertThat(event.bookingId()).isEqualTo(1L);
        assertThat(event.amount()).isEqualByComparingTo("100000");
        assertThat(event.status()).isEqualTo(PaymentCompletedEvent.STATUS_SUCCESS);
    }

    @Test
    void webhookIgnoresOutgoingTransfer() {
        SePayWebhookDto dto = new SePayWebhookDto();
        dto.setTransferType("out");
        dto.setContent("DH1");
        dto.setTransferAmount(new BigDecimal("100000"));

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void webhookIgnoresContentWithoutBookingReference() {
        SePayWebhookDto dto = new SePayWebhookDto();
        dto.setTransferType("in");
        dto.setContent("khong co ma doi soat");
        dto.setTransferAmount(new BigDecimal("100000"));

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
    }
}
