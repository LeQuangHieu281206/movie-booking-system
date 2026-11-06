package com.ttcsn.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.config.RabbitMQConfig;
import com.ttcsn.payment.config.SepayProperties;
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
import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock
    private SepayProperties sepayProperties;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService service;

    @BeforeEach
    void setUp() {
        lenient().when(sepayProperties.accountNumber()).thenReturn("1234567890");
        lenient().when(sepayProperties.bankName()).thenReturn("MBBank");
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
        Payment pending = Payment.builder().paymentId(9L).bookingId(1L).status(PaymentStatus.PENDING).build();
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByPaymentIdDesc(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.of(pending));

        String qrUrl = service.createSePayPayment(1L, 7L);

        assertThat(qrUrl).contains("amount=100000").contains("des=DH1");
        verify(paymentRepository).upsertPendingPayment(eq(1L), any(), eq("DH1"), anyString());
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
    void webhookPublishesSuccessEventForMatchingIncomingTransfer() {
        SePayWebhookDto dto = webhook(9001L, "DH1 nguoi chuyen khoan", "100000");
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PENDING"));
        Payment pending = Payment.builder()
                .paymentId(5L)
                .bookingId(1L)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByPaymentIdDesc(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.of(pending));

        service.processSePayWebhook(dto);

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(Topics.PAYMENT_COMPLETED), eventCaptor.capture());
        PaymentCompletedEvent event = eventCaptor.getValue();
        assertThat(event.bookingId()).isEqualTo(1L);
        assertThat(event.userId()).isEqualTo(7L);
        assertThat(event.amount()).isEqualByComparingTo("100000");
        assertThat(event.status()).isEqualTo(PaymentCompletedEvent.STATUS_SUCCESS);

        verify(paymentRepository).save(pending);
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(pending.getProviderTxnId()).isEqualTo("9001");
        assertThat(pending.getPaidAt()).isNotNull();
        assertThat(pending.getRawPayload()).isNotNull();
    }

    @Test
    void webhookCreatesSuccessAtomicallyWhenNoPendingRowExists() {
        SePayWebhookDto dto = webhook(9005L, "DH1", "100000");
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PENDING"));
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByPaymentIdDesc(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.insertSuccessIfAbsent(
                eq(1L), any(), eq("9005"), eq("DH1"), anyString(), any()))
                .thenReturn(1);

        service.processSePayWebhook(dto);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE), eq(Topics.PAYMENT_COMPLETED), any(PaymentCompletedEvent.class));
        verify(paymentRepository).insertSuccessIfAbsent(
                eq(1L), any(), eq("9005"), eq("DH1"), anyString(), any());
    }

    @Test
    void webhookDoesNotPublishWhenConcurrentInsertLoses() {
        SePayWebhookDto dto = webhook(9006L, "DH1", "100000");
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PENDING"));
        when(paymentRepository.findFirstByBookingIdAndStatusOrderByPaymentIdDesc(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.insertSuccessIfAbsent(
                eq(1L), any(), eq("9006"), eq("DH1"), anyString(), any()))
                .thenReturn(0);

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
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
        when(paymentRepository.insertFailedIfAbsent(
                eq(1L), any(), eq("9002"), eq("DH1"), anyString(), anyString()))
                .thenReturn(1);

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
        verify(paymentRepository).insertFailedIfAbsent(
                eq(1L), any(), eq("9002"), eq("DH1"), anyString(), anyString());
    }

    @Test
    void webhookRecordsFailedPaymentWhenBookingIsNotPending() {
        SePayWebhookDto dto = webhook(9007L, "DH1", "100000");
        when(bookingClient.getBooking(1L))
                .thenReturn(booking(1L, 7L, "100000", "PAID"));
        when(paymentRepository.insertFailedIfAbsent(
                eq(1L), any(), eq("9007"), eq("DH1"), anyString(), anyString()))
                .thenReturn(1);

        service.processSePayWebhook(dto);

        verifyNoInteractions(rabbitTemplate);
        verify(paymentRepository).insertFailedIfAbsent(
                eq(1L), any(), eq("9007"), eq("DH1"), anyString(), anyString());
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
