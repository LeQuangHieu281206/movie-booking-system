package com.ttcsn.booking.scheduling;

import com.ttcsn.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingExpirationSchedulerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingExpirationScheduler scheduler;

    @Test
    void delegatesToServiceAndLogsWhenBookingsExpired() {
        when(bookingService.expireStalePendingBookings()).thenReturn(3);

        scheduler.expireStalePendingBookings();

        verify(bookingService).expireStalePendingBookings();
    }

    @Test
    void stillCallsServiceWhenNothingToExpire() {
        when(bookingService.expireStalePendingBookings()).thenReturn(0);

        scheduler.expireStalePendingBookings();

        verify(bookingService).expireStalePendingBookings();
    }
}
