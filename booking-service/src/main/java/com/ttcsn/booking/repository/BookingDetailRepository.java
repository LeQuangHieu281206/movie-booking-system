package com.ttcsn.booking.repository;

import com.ttcsn.booking.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.time.OffsetDateTime;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    List<BookingDetail> findByBooking_BookingId(Long bookingId);

    /** Tất cả ghế của nhiều booking trong một query (tránh N+1 khi trả danh sách vé). */
    List<BookingDetail> findByBooking_BookingIdIn(Collection<Long> bookingIds);

    /** Số ghế đã bán trong kỳ (tính theo booking đã thanh toán từ mốc :from). */
    @Query("""
            SELECT COUNT(bd) FROM BookingDetail bd
            WHERE bd.booking.status = com.ttcsn.booking.entity.enums.BookingStatus.PAID
              AND bd.booking.bookingTime >= :from
            """)
    long countPaidSeatsSince(@Param("from") OffsetDateTime from);
}
