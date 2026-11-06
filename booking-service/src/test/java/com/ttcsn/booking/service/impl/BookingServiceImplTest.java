package com.ttcsn.booking.service.impl;

import com.ttcsn.booking.client.MovieClient;
import com.ttcsn.booking.client.ShowtimeClient;
import com.ttcsn.booking.client.UserClient;
import com.ttcsn.booking.client.VenueClient;
import com.ttcsn.booking.dto.request.BookingCreateRequest;
import com.ttcsn.booking.dto.request.BookingUpdateRequest;
import com.ttcsn.booking.dto.response.BookingResponse;
import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.entity.BookingDetail;
import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.entity.enums.PaymentMethod;
import com.ttcsn.booking.exception.BookingNotFoundException;
import com.ttcsn.booking.repository.BookingDetailRepository;
import com.ttcsn.booking.repository.BookingRepository;
import com.ttcsn.booking.service.BookingSeatReleaser;
import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.dto.RoomDto;
import com.ttcsn.common.dto.SeatHoldResult;
import com.ttcsn.common.dto.SeatIdsRequest;
import com.ttcsn.common.dto.ShowtimeView;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingDetailRepository bookingDetailRepository;
    @Mock
    private UserClient userClient;
    @Mock
    private MovieClient movieClient;
    @Mock
    private VenueClient venueClient;
    @Mock
    private ShowtimeClient showtimeClient;
    @Mock
    private BookingSeatReleaser seatReleaser;

    @InjectMocks
    private BookingServiceImpl service;

    @Test
    void createBookingUsesHeaderUserIdAndServerPrice() {
        when(userClient.userExists(7L)).thenReturn(true);
        when(movieClient.getShowtimeById(10L)).thenReturn(showtime());
        when(venueClient.getRoom(2L)).thenReturn(new RoomDto(2L, "R1", "2D", 100, 3L, "C", "A", "H"));
        when(showtimeClient.holdSeats(any(SeatIdsRequest.class)))
                .thenReturn(SeatHoldResult.ok(Map.of(100L, 1000L, 101L, 1001L)));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setBookingId(99L);
            return booking;
        });

        // Client cũ có thể gửi userId=999, totalAmount=1, holdBy giả; record mới bỏ hẳn các field này.
        BookingCreateRequest request = new BookingCreateRequest(
                10L, List.of(100L, 100L, 101L), PaymentMethod.CASH);

        BookingResponse response = service.createBooking(request, 7L);

        assertEquals(7L, response.userId());
        assertEquals(0, new BigDecimal("100000").compareTo(response.totalAmount()));

        ArgumentCaptor<SeatIdsRequest> holdCaptor = ArgumentCaptor.forClass(SeatIdsRequest.class);
        verify(showtimeClient).holdSeats(holdCaptor.capture());
        assertThat(holdCaptor.getValue().seatIds()).containsExactly(100L, 101L);
        assertEquals("7", holdCaptor.getValue().holdBy(), "holdBy lấy từ userId, không lấy từ client");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingDetail>> detailsCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookingDetailRepository).saveAll(detailsCaptor.capture());
        assertThat(detailsCaptor.getValue()).hasSize(2)
                .allMatch(detail -> new BigDecimal("50000").compareTo(detail.getPrice()) == 0);
        assertThat(detailsCaptor.getValue())
                .extracting(BookingDetail::getShowtimeSeatId)
                .containsExactly(1000L, 1001L);
    }

    @Test
    void customerCannotReadAnotherUsersBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(1L, 2L, BookingStatus.PENDING)));

        assertThrows(BookingNotFoundException.class,
                () -> service.getBookingById(1L, 1L, "CUSTOMER"));
    }

    @Test
    void adminCanReadAnotherUsersBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(1L, 2L, BookingStatus.PENDING)));

        BookingResponse response = service.getBookingById(1L, 1L, "ADMIN");

        assertEquals(2L, response.userId());
    }

    @Test
    void customerBookingListIgnoresClientUserIdParameter() {
        when(bookingRepository.findByUserId(1L))
                .thenReturn(List.of(booking(1L, 1L, BookingStatus.PENDING)));

        List<BookingResponse> responses = service.getBookingsByUserId(2L, 1L, "CUSTOMER");

        assertThat(responses).allMatch(response -> response.userId() == 1L);
        verify(bookingRepository).findByUserId(1L);
        verify(bookingRepository, never()).findByUserId(2L);
    }

    @Test
    void adminCanListBookingsByArbitraryUserId() {
        when(bookingRepository.findByUserId(2L))
                .thenReturn(List.of(booking(1L, 2L, BookingStatus.PENDING)));

        List<BookingResponse> responses = service.getBookingsByUserId(2L, 99L, "ADMIN");

        assertThat(responses).extracting(BookingResponse::userId).containsExactly(2L);
    }

    @Test
    void customerCannotListAllBookingsOrByShowtime() {
        assertThrows(ForbiddenException.class, () -> service.getAllBookings("CUSTOMER"));
        assertThrows(ForbiddenException.class,
                () -> service.getBookingsByShowtimeId(10L, "CUSTOMER"));
        verify(bookingRepository, never()).findAll();
        verify(bookingRepository, never()).findByShowtimeId(10L);
    }

    @Test
    void customerCanCancelOwnPendingBookingAndSeatsAreReleased() {
        Booking booking = booking(1L, 1L, BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = service.updateBooking(
                1L, new BookingUpdateRequest(BookingStatus.CANCELLED), 1L, "CUSTOMER");

        assertEquals(BookingStatus.CANCELLED, response.status());
        verify(seatReleaser).release(booking);
    }

    @Test
    void clientCannotSetPaidStatus() {
        Booking booking = booking(1L, 1L, BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(ConflictException.class, () -> service.updateBooking(
                1L, new BookingUpdateRequest(BookingStatus.PAID), 1L, "CUSTOMER"));

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void customerCannotCancelAnotherUsersBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(1L, 2L, BookingStatus.PENDING)));

        assertThrows(BookingNotFoundException.class, () -> service.updateBooking(
                1L, new BookingUpdateRequest(BookingStatus.CANCELLED), 1L, "CUSTOMER"));
    }

    @Test
    void customerCannotSetExpiredStatus() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(1L, 1L, BookingStatus.PENDING)));

        assertThrows(ForbiddenException.class, () -> service.updateBooking(
                1L, new BookingUpdateRequest(BookingStatus.EXPIRED), 1L, "CUSTOMER"));
    }

    @Test
    void customerCannotDeleteAnotherUsersBooking() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(1L, 2L, BookingStatus.PENDING)));

        assertThrows(BookingNotFoundException.class,
                () -> service.deleteBooking(1L, 1L, "CUSTOMER"));
        verify(bookingRepository, never()).delete(any(Booking.class));
    }

    @Test
    void customerCanDeleteOwnPendingBookingAndSeatsAreReleased() {
        Booking booking = booking(1L, 1L, BookingStatus.PENDING);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        service.deleteBooking(1L, 1L, "CUSTOMER");

        verify(seatReleaser).release(booking);
        verify(bookingRepository).delete(booking);
    }

    @Test
    void deletingPaidBookingDoesNotReleaseSeats() {
        Booking booking = booking(1L, 1L, BookingStatus.PAID);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        service.deleteBooking(1L, 1L, "ADMIN");

        verifyNoInteractions(seatReleaser);
        verify(bookingRepository).delete(booking);
    }

    @Test
    void paymentViewReturnsServerStoredAmountAndStatus() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking(1L, 2L, BookingStatus.PENDING)));

        BookingPaymentView view = service.getPaymentView(1L);

        assertEquals(1L, view.bookingId());
        assertEquals(2L, view.userId());
        assertEquals(0, new BigDecimal("100000").compareTo(view.totalAmount()));
        assertEquals("PENDING", view.status());
    }

    private static Booking booking(Long bookingId, Long userId, BookingStatus status) {
        return Booking.builder()
                .bookingId(bookingId)
                .userId(userId)
                .showtimeId(10L)
                .bookingCode("BK-" + bookingId)
                .totalAmount(new BigDecimal("100000"))
                .status(status)
                .paymentMethod(PaymentMethod.CASH)
                .bookingTime(OffsetDateTime.now())
                .build();
    }

    // ---------- expireStalePendingBookings (scheduler hết hạn) ----------

    private void expireTestSetup() {
        org.springframework.test.util.ReflectionTestUtils
                .setField(service, "pendingExpiryMinutes", 15L);
    }

    @Test
    void expiresStalePendingBookingAndReleasesSeats() {
        expireTestSetup();
        Booking stale = booking(1L, 2L, BookingStatus.PENDING);
        when(bookingRepository.findByStatusAndBookingTimeBefore(
                org.mockito.ArgumentMatchers.eq(BookingStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of(stale));
        when(bookingRepository.expireIfPending(1L)).thenReturn(1);

        int expired = service.expireStalePendingBookings();

        assertEquals(1, expired);
        verify(seatReleaser).release(stale);
    }

    @Test
    void skipsBookingWhoseStatusChangedConcurrently() {
        expireTestSetup();
        Booking stale = booking(1L, 2L, BookingStatus.PENDING);
        when(bookingRepository.findByStatusAndBookingTimeBefore(
                org.mockito.ArgumentMatchers.eq(BookingStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of(stale));
        // Event thanh toán đã xử lý trước (PAID/CANCELLED) -> guarded update không ảnh hưởng dòng nào
        when(bookingRepository.expireIfPending(1L)).thenReturn(0);

        int expired = service.expireStalePendingBookings();

        assertEquals(0, expired);
        verifyNoInteractions(seatReleaser);
    }

    @Test
    void returnsZeroWhenNoStaleBooking() {
        expireTestSetup();
        when(bookingRepository.findByStatusAndBookingTimeBefore(
                org.mockito.ArgumentMatchers.eq(BookingStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        assertEquals(0, service.expireStalePendingBookings());
        verifyNoInteractions(showtimeClient);
    }

    @Test
    void statsFillSevenDaysAndIgnoreMissingDays() {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        java.time.LocalDate yesterday = today.minusDays(1);
        when(bookingRepository.sumPaidRevenueByDay(any()))
                .thenReturn(List.<Object[]>of(new Object[]{
                        java.sql.Date.valueOf(yesterday), new BigDecimal("250000"), 2L}));
        when(bookingRepository.sumPaidRevenue()).thenReturn(new BigDecimal("250000"));
        when(bookingRepository.countByStatus(BookingStatus.PAID)).thenReturn(2L);
        when(bookingDetailRepository.countPaidSeats()).thenReturn(3L);

        var stats = service.getBookingStats("ADMIN");

        assertThat(stats.dailyRevenue()).hasSize(7);
        assertThat(stats.dailyRevenue().get(5).date()).isEqualTo(yesterday.toString());
        assertThat(stats.dailyRevenue().get(5).revenue()).isEqualByComparingTo("250000");
        assertThat(stats.dailyRevenue().get(6).revenue()).isEqualByComparingTo("0");
        assertThat(stats.ticketsSold()).isEqualTo(3);
    }

    @Test
    void statsAreAdminOnly() {
        assertThrows(ForbiddenException.class, () -> service.getBookingStats("CUSTOMER"));
        verifyNoInteractions(bookingRepository);
    }

    private static ShowtimeView showtime() {
        return new ShowtimeView(
                10L, 1L, 2L, "R1", "2D", 3L, "C", "A", "H",
                "2026-10-25T10:00:00+07:00", "2026-10-25T12:00:00+07:00",
                new BigDecimal("50000"), "SCHEDULED");
    }
}
