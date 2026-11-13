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
    List<Booking> findByUserId(Long userId);
    List<Booking> findByShowtimeId(Long showtimeId);

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

    /**
     * Doanh thu theo mốc thời gian (giờ Việt Nam) của các booking đã thanh toán.
     * Mốc trả về dạng text để service dùng thẳng làm key:
     * YYYY-MM-DD HH (theo giờ) / YYYY-MM-DD (theo ngày) / YYYY-MM (theo tháng).
     */
    @Query(value = """
            SELECT to_char(booking_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD HH24') AS bucket,
                   COALESCE(SUM(total_amount), 0) AS revenue,
                   COUNT(*) AS bookings
            FROM bookings
            WHERE status = 'PAID' AND booking_time >= :from
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<Object[]> sumPaidRevenueByHour(@Param("from") OffsetDateTime from);

    /** Như sumPaidRevenueByHour nhưng gom theo ngày. */
    @Query(value = """
            SELECT to_char(booking_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD') AS bucket,
                   COALESCE(SUM(total_amount), 0) AS revenue,
                   COUNT(*) AS bookings
            FROM bookings
            WHERE status = 'PAID' AND booking_time >= :from
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<Object[]> sumPaidRevenueByDay(@Param("from") OffsetDateTime from);

    /** Như sumPaidRevenueByHour nhưng gom theo tháng. */
    @Query(value = """
            SELECT to_char(booking_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') AS bucket,
                   COALESCE(SUM(total_amount), 0) AS revenue,
                   COUNT(*) AS bookings
            FROM bookings
            WHERE status = 'PAID' AND booking_time >= :from
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<Object[]> sumPaidRevenueByMonth(@Param("from") OffsetDateTime from);
}
