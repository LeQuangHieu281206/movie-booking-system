package com.ttcsn.booking.repository;

import com.ttcsn.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);        // SOA: thay findByUser_UserId (bỏ ManyToOne)
    List<Booking> findByShowtimeId(Long showtimeId); // SOA: thay findByShowtime_ShowtimeId
    boolean existsByBookingCode(String bookingCode);
}
