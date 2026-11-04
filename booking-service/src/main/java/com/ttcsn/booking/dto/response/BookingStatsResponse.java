package com.ttcsn.booking.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thống kê bán vé: chỉ tính booking PAID để không lẫn vé chờ thanh toán/đã huỷ.
 * dailyRevenue luôn đủ 7 ngày gần nhất, ngày không phát sinh doanh thu có giá trị 0.
 */
public record BookingStatsResponse(
        BigDecimal revenue,
        long paidBookings,
        long ticketsSold,
        List<DailyRevenue> dailyRevenue
) {
    public record DailyRevenue(
            String date,
            BigDecimal revenue,
            long paidBookings
    ) {
    }
}
