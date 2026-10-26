package com.ttcsn.booking.repository;

import com.ttcsn.booking.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    List<BookingDetail> findByBooking_BookingId(Long bookingId);

    List<BookingDetail> findByShowtimeSeatId(Long showtimeSeatId);
}
