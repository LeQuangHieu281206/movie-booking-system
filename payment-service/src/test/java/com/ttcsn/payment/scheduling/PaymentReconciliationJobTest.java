package com.ttcsn.payment.scheduling;

import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.entity.Payment;
import com.ttcsn.payment.entity.PaymentStatus;
import com.ttcsn.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationJobTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingClient bookingClient;

    @InjectMocks
    private PaymentReconciliationJob job;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(job, "pendingReconcileMinutes", 2L);
    }

    private Payment pendingPayment(long paymentId, long bookingId) {
        return Payment.builder()
                .paymentId(paymentId)
                .bookingId(bookingId)
                .amount(BigDecimal.valueOf(180_000))
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void closesPaymentWhenBookingCancelled() {
        Payment payment = pendingPayment(10L, 30L);
        when(paymentRepository.findTop50ByStatusAndCreatedAtBeforeOrderByPaymentIdAsc(
                eq(PaymentStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of(payment));
        when(bookingClient.getBooking(30L))
                .thenReturn(new BookingPaymentView(30L, 1L, BigDecimal.TEN, "CANCELLED"));
        when(paymentRepository.closeIfPending(eq(10L), eq(PaymentStatus.CANCELLED), any()))
                .thenReturn(1);

        job.closePendingPaymentsOfClosedBookings();

        verify(paymentRepository).closeIfPending(eq(10L), eq(PaymentStatus.CANCELLED), any());
    }

    @Test
    void closesPaymentAsExpiredWhenBookingExpired() {
        Payment payment = pendingPayment(11L, 31L);
        when(paymentRepository.findTop50ByStatusAndCreatedAtBeforeOrderByPaymentIdAsc(
                eq(PaymentStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of(payment));
        when(bookingClient.getBooking(31L))
                .thenReturn(new BookingPaymentView(31L, 1L, BigDecimal.TEN, "EXPIRED"));
        when(paymentRepository.closeIfPending(eq(11L), eq(PaymentStatus.EXPIRED), any()))
                .thenReturn(1);

        job.closePendingPaymentsOfClosedBookings();

        verify(paymentRepository).closeIfPending(eq(11L), eq(PaymentStatus.EXPIRED), any());
    }

    @Test
    void keepsPaymentWhenBookingStillPending() {
        Payment payment = pendingPayment(12L, 32L);
        when(paymentRepository.findTop50ByStatusAndCreatedAtBeforeOrderByPaymentIdAsc(
                eq(PaymentStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of(payment));
        when(bookingClient.getBooking(32L))
                .thenReturn(new BookingPaymentView(32L, 1L, BigDecimal.TEN, "PENDING"));

        job.closePendingPaymentsOfClosedBookings();

        verify(paymentRepository, never()).closeIfPending(any(), any(), any());
    }

    @Test
    void skipsWhenBookingServiceUnavailable() {
        Payment payment = pendingPayment(13L, 33L);
        when(paymentRepository.findTop50ByStatusAndCreatedAtBeforeOrderByPaymentIdAsc(
                eq(PaymentStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of(payment));
        when(bookingClient.getBooking(33L)).thenThrow(new RuntimeException("down"));

        job.closePendingPaymentsOfClosedBookings();

        verify(paymentRepository, never()).closeIfPending(any(), any(), any());
    }
}
