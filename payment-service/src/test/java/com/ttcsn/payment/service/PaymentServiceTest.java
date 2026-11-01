package com.ttcsn.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.config.RabbitMQConfig;
import com.ttcsn.payment.dto.request.SePayWebhookDto;
import com.ttcsn.payment.entity.Payment;
import com.ttcsn.payment.entity.PaymentStatus;
import com.ttcsn.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingClient bookingClient;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private PaymentRepository paymentRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService service;

    @BeforeEach
    void setUp() {
        lenient().when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static BookingPaymentView booking(Long bookingId, Long userId, String amount, String status) {
        return new BookingPaymentView(bookingId, userId, amount == null ? null : new BigDecimal(amount), status);
    }

    private static SePayWebhookDto webhook(Long txnId, String content, String amount) {
        SePayWebhookDto dto = new SePayWebhookDto();
        dto.setId(txnId);
        dto.setTransferType("in");
        dto.setContent(content);
        dto.setTransferAmount(amount == null ? null : new BigDecimal(amount));
        return dto;
    }

    // ---------- createSePayPayment ----------

    @Test
    void generatesQrForOwnPendingBookingUsingServerAmount() {
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PENDING"));
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByPaymentIdDesc(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());

        String qrUrl = service.createSePayPayment(1L, 7L);

        assertThat(qrUrl).contains("amount=100000").contains("des=DH1");
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("100000");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void reusesExistingPendingPaymentWhenCreatingQrAgain() {
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PENDING"));
        Payment pending = Payment.builder().paymentId(9L).bookingId(1L).status(PaymentStatus.PENDING).build();
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByPaymentIdDesc(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.of(pending));

        service.createSePayPayment(1L, 7L);

        verify(paymentRepository).save(pending);
        assertThat(pending.getQrUrl()).contains("des=DH1");
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
    void webhookPublishesSuccessEventForMatchingIncomingTransfer() {
        SePayWebhookDto dto = webhook(9001L, "DH1 nguoi chuyen khoan", "100000");
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PENDING"));
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByPaymentIdDesc(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());

        service.processSePayWebhook(dto);

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(Topics.PAYMENT_COMPLETED), eventCaptor.capture());
        PaymentCompletedEvent event = eventCaptor.getValue();
        assertThat(event.bookingId()).isEqualTo(1L);
        assertThat(event.userId()).isEqualTo(7L);
        assertThat(event.amount()).isEqualByComparingTo("100000");
        assertThat(event.status()).isEqualTo(PaymentCompletedEvent.STATUS_SUCCESS);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(saved.getProviderTxnId()).isEqualTo("9001");
        assertThat(saved.getPaidAt()).isNotNull();
        assertThat(saved.getRawPayload()).isNotNull();
    }

    @Test
    void webhookIgnoresDuplicateProviderTransaction() {
        SePayWebhookDto dto = webhook(9001L, "DH1", "100000");
        when(paymentRepository.existsByProviderAndProviderTxnId("SEPAY", "9001")).thenReturn(true);

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(bookingClient);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void webhookRecordsFailedPaymentWhenAmountDoesNotMatch() {
        SePayWebhookDto dto = webhook(9002L, "DH1", "50000");
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PENDING"));

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(captor.getValue().getFailureReason()).contains("Số tiền không khớp");
    }

    @Test
    void webhookIgnoresOutgoingTransfer() {
        SePayWebhookDto dto = webhook(9003L, "DH1", "100000");
        dto.setTransferType("out");

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(bookingClient);
    }

    @Test
    void webhookIgnoresContentWithoutBookingReference() {
        SePayWebhookDto dto = webhook(9004L, "khong co ma doi soat", "100000");

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(bookingClient);
    }
}
