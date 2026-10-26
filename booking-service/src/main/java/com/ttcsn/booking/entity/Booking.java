package com.ttcsn.booking.entity;

import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.entity.enums.PaymentMethod;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
/**
 *
 * @author quanghieu
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    // SOA: User thuộc user-service, Showtime thuộc movie-service -> FK id thuần (bỏ @ManyToOne)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "booking_code", nullable = false, unique = true, columnDefinition = "text")
    private String bookingCode;

    @Column(name = "total_amount", nullable = false, columnDefinition = "numeric")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(32)")
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", columnDefinition = "varchar(32)")
    private PaymentMethod paymentMethod;

    @Column(name = "booking_time")
    private OffsetDateTime bookingTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
