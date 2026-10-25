/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.entity;

import com.ttcsn.movie.showtime.entity.enums.SeatStatus;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
/**
 *
 * @author quanghieu
 */
@Entity
@Table(name = "showtime_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_showtime_seats_showtime_seat",
                columnNames = {"showtime_id", "seat_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowtimeSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "showtime_seat_id")
    private Long showtimeSeatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    // SOA: Seat thuộc venue-service -> chỉ giữ FK id (bỏ @ManyToOne)
    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    /** Trạng thái ghế lưu varchar (enum Postgres dễ bị drop-recreate phá cột theo cascade). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(32)")
    private SeatStatus status;

    /** Thời điểm hết hiệu lực giữ chỗ tạm (HOLDING) — quá hạn thì xem như ghế trống. */
    @Column(name = "hold_until")
    private OffsetDateTime holdUntil;

    /** Định danh người giữ/chốt ghế: userId hoặc mã phiên client. */
    @Column(name = "hold_by", length = 64)
    private String holdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
