package com.ttcsn.common.config;

import org.hibernate.dialect.PostgreSQLDialect;

/**
 * PostgreSQLDialect mặc định lấy tên class Java làm tên kiểu khi sinh CAST cho
 * enum, ví dụ {@code 'PAID'::BookingStatus}. Các enum type trong database dùng
 * tên snake_case ({@code booking_status_enum}) nên mọi literal/parameter enum
 * trong JPQL sẽ lỗi 42704 "type ... does not exist". Lớp này ánh xạ tên enum
 * Java sang đúng tên type của database.
 */
public class CineRoomPostgresDialect extends PostgreSQLDialect {

    @Override
    public String getEnumTypeDeclaration(Class<? extends Enum<?>> enumClass) {
        return switch (enumClass.getSimpleName()) {
            case "UserRole" -> "user_role_enum";
            case "MovieStatus" -> "movie_status_enum";
            case "AgeRestriction" -> "age_restriction_enum";
            case "RoomType" -> "room_type_enum";
            case "SeatType" -> "seat_type_enum";
            case "ShowtimeStatus" -> "showtime_status_enum";
            case "SeatStatus" -> "seat_status_enum";
            case "BookingStatus" -> "booking_status_enum";
            case "PaymentMethod" -> "payment_method_enum";
            case "PaymentStatus" -> "payment_status_enum";
            default -> super.getEnumTypeDeclaration(enumClass);
        };
    }
}
