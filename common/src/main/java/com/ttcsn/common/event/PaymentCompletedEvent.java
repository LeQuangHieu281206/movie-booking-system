package com.ttcsn.common.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * SOA Step 3 — Event định nghĩa cấu trúc sự kiện truyền tải qua Message Broker (RabbitMQ)
 * khi payment-service hoàn tất quá trình xử lý thanh toán.
 * 
 * <p>Được tiêu thụ bởi (Consumers):
 * <ul>
 *   <li><b>booking-service:</b> Cập nhật trạng thái đơn hàng (Booking.status -> PAID / CANCELLED).</li>
 *   <li><b>notification-service:</b> Kích hoạt tiến trình gửi biên lai điện tử/email vé cho khách hàng.</li>
 * </ul>
 * 
 * <p>Lưu ý kỹ thuật: Sử dụng Jackson2JsonMessageConverter, cấu hình yêu cầu ObjectMapper
 * phải tích hợp JavaTimeModule để serialize/deserialize đối tượng OffsetDateTime chính xác.
 * 
 * @author Nhóm phát triển (Module Common)
 */
public record PaymentCompletedEvent(
        
        /** Định danh duy nhất của giao dịch thanh toán (UUID) */
        UUID paymentId,
        
        /** Mã định danh của đơn đặt vé (Ánh xạ với bảng Booking) */
        Long bookingId,
        
        /** Mã định danh người dùng thực hiện giao dịch */
        Long userId,
        
        /** Tổng số tiền thanh toán thực tế */
        BigDecimal amount,
        
        /** Thời điểm ghi nhận giao dịch hoàn thành (chuẩn ISO-8601 có múi giờ) */
        OffsetDateTime paymentTime,
        
        /** Trạng thái giao dịch: SUCCESS (Thành công) hoặc FAILED (Thất bại) */
        String status
) {
    // Các hằng số định nghĩa trạng thái giao dịch chuẩn hóa toàn hệ thống
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED  = "FAILED";

    /**
     * Phương thức tiện ích kiểm tra xem giao dịch có thành công hay không.
     * @return true nếu trạng thái là SUCCESS, ngược lại là false.
     */
    public boolean isSuccessful() {
        return STATUS_SUCCESS.equalsIgnoreCase(this.status);
    }
}