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

    // SOA: Booking cùng module; ShowtimeSeat thuộc movie-service -> FK id thuần
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "showtime_seat_id", nullable = false)
    private Long showtimeSeatId;

    @Column(nullable = false, columnDefinition = "numeric")
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
