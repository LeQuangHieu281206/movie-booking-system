package com.ttcsn.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "booking_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_detail_id")
    private Long bookingDetailId;

    // Booking cùng module booking-service -> quan hệ entity thật
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * Ghế tham chiếu theo mô hình tương tác: id của bản ghi showtime_seats (movie-service),
     * khớp FK booking_details_showtime_seat_id_fkey trong DB.
     */
    @Column(name = "showtime_seat_id", nullable = false)
    private Long showtimeSeatId;

    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
