package com.ttcsn.booking.service;

import com.ttcsn.booking.client.ShowtimeClient;
import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.entity.BookingDetail;
import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.entity.enums.PaymentMethod;
import com.ttcsn.booking.repository.BookingDetailRepository;
import com.ttcsn.booking.repository.BookingRepository;
import com.ttcsn.common.dto.SeatIdsRequest;
import com.ttcsn.common.event.PaymentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCompletedListenerTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private ShowtimeClient showtimeClient;

    @InjectMocks
    private PaymentCompletedListener listener;

    @Test
    void pendingBookingBecomesPaid() {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        listener.onPaymentCompleted(event(PaymentCompletedEvent.STATUS_SUCCESS));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAID);
        verify(bookingRepository).save(booking);
    }

    @Test
    void nonPendingBookingIsIgnored() {
        Booking booking = booking(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        listener.onPaymentCompleted(event(PaymentCompletedEvent.STATUS_SUCCESS));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void missingBookingIsIgnored() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        listener.onPaymentCompleted(event(PaymentCompletedEvent.STATUS_SUCCESS));

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void failedPendingBookingIsCancelledAndSeatsReleased() {
        Booking booking = booking(BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingDetailRepository.findByBookingId(1L))
                .thenReturn(List.of(BookingDetail.builder().bookingId(1L).seatId(100L).build()));

        listener.onPaymentCompleted(event(PaymentCompletedEvent.STATUS_FAILED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
        ArgumentCaptor<SeatIdsRequest> captor = ArgumentCaptor.forClass(SeatIdsRequest.class);
        verify(showtimeClient).releaseSeats(captor.capture());
        assertThat(captor.getValue().seatIds()).containsExactly(100L);
    }

    private static Booking booking(BookingStatus status) {
        return Booking.builder()
                .bookingId(1L)
                .userId(7L)
                .showtimeId(10L)
                .bookingCode("BK-1")
                .totalAmount(new BigDecimal("100000"))
                .status(status)
                .paymentMethod(PaymentMethod.CASH)
                .bookingTime(OffsetDateTime.now())
                .build();
    }

    private static PaymentCompletedEvent event(String status) {
        return new PaymentCompletedEvent(
                UUID.randomUUID(), 1L, 7L, new BigDecimal("100000"),
                OffsetDateTime.now(), status);
    }
}
