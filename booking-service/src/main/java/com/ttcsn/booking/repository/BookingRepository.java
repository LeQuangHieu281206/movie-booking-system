package com.ttcsn.booking.repository;

import com.ttcsn.booking.entity.Booking;
import com.ttcsn.booking.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);        // SOA: thay findByUser_UserId (bỏ ManyToOne)
    List<Booking> findByShowtimeId(Long showtimeId); // SOA: thay findByShowtime_ShowtimeId
    boolean existsByBookingCode(String bookingCode);

    /** Booking PENDING đã quá hạn chờ thanh toán — nguồn cho scheduler hết hạn. */
    List<Booking> findByStatusAndBookingTimeBefore(BookingStatus status, OffsetDateTime cutoff);

    /**
     * Chuyển PENDING -> EXPIRED có điều kiện: chỉ ảnh hưởng 1 dòng nếu booking
     * vẫn còn PENDING. Trả 0 khi event thanh toán/scheduler khác đã xử lý trước
     * (chống race giữa scheduler và PaymentCompletedListener).
     */
    @Modifying
    @Query("""
            UPDATE Booking b SET b.status = com.ttcsn.booking.entity.enums.BookingStatus.EXPIRED
            WHERE b.bookingId = :bookingId AND b.status = com.ttcsn.booking.entity.enums.BookingStatus.PENDING
            """)
    int expireIfPending(@Param("bookingId") Long bookingId);
}
