/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.venue.entity;

import jakarta.persistence.*;

import com.ttcsn.venue.entity.enums.RoomType;
import com.ttcsn.venue.entity.enums.RoomTypeConverter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;

import java.time.OffsetDateTime;
/**
 *
 * @author quanghieu
 */
@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "cinema")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "name", nullable = false, columnDefinition = "text")
    private String name;

    @Column(name = "total_seats")
    private Short totalSeats;

    // RoomType dùng nhãn '2D'/'3D'... khác tên hằng Java nên vẫn đi qua converter.
    @Convert(converter = RoomTypeConverter.class)
    @ColumnTransformer(write = "?::room_type_enum")
    @Column(name = "room_type", columnDefinition = "room_type_enum")
    private RoomType roomType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}