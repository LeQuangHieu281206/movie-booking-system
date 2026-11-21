package com.ttcsn.booking.service;

import com.ttcsn.booking.dto.request.BookingCreateRequest;
import com.ttcsn.booking.dto.request.BookingUpdateRequest;
import com.ttcsn.booking.dto.response.BookingResponse;
import com.ttcsn.booking.dto.response.BookingStatsResponse;
import com.ttcsn.common.dto.BookingPaymentView;

import java.util.List;

public interface BookingService {
    List<BookingResponse> getAllBookings(String role);
    BookingResponse getBookingById(Long id, Long userId, String role);
    List<BookingResponse> getBookingsByUserId(Long requestedUserId, Long currentUserId, String role);
    List<BookingResponse> getBookingsByShowtimeId(Long showtimeId, String role);
    BookingStatsResponse getBookingStats(String role, String period);
    BookingResponse createBooking(BookingCreateRequest request, Long userId);
    BookingResponse updateBooking(Long id, BookingUpdateRequest request, Long userId, String role);

    /**
     * Admin xác nhận đã thu tiền mặt tại quầy: PENDING -> PAID cho booking CASH.
     * PAID qua API thường bị chặn vì chỉ payment listener được đặt, nên vé tiền
     * mặt cần lối xác nhận riêng này.
     */
    BookingResponse confirmCashPayment(Long id, String role);

    void deleteBooking(Long id, Long userId, String role);
    BookingPaymentView getPaymentView(Long id);

    /**
     * Hết hạn các booking PENDING quá hạn chờ thanh toán (scheduler gọi định kỳ):
     * PENDING -> EXPIRED rồi nhả ghế. Trả số booking đã hết hạn thành công.
     */
    int expireStalePendingBookings();
}
