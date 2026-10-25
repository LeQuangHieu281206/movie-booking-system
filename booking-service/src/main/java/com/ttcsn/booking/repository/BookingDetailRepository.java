package com.ttcsn.booking.repository;

import com.ttcsn.booking.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    List<BookingDetail> findByBookingId(Long bookingId); // SOA: thay findByBooking_BookingId

    List<BookingDetail> findBySeatId(Long seatId); // mô hình showtime_seats theo tương tác: ghế vật lý của venue
}
