package com.ttcsn.booking.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thống kê bán vé: chỉ tính booking PAID để không lẫn vé chờ thanh toán/đã huỷ.
 * Mỗi kỳ là một đơn vị thời gian đang diễn ra (hôm nay / tháng này / năm nay);
 * revenue, paidBookings, ticketsSold là tổng của đúng kỳ đó, khớp với revenueSeries.
 * revenueSeries luôn đủ số mốc của kỳ (day: 24 giờ, month: số ngày của tháng,
 * year: 12 tháng); mốc không phát sinh doanh thu có giá trị 0.
 * key của mốc là "YYYY-MM-DD HH" (day), YYYY-MM-DD (month) hoặc YYYY-MM (year)
 * theo giờ Việt Nam.
 */
public record BookingStatsResponse(
        BigDecimal revenue,
        long paidBookings,
        long ticketsSold,
        String period,
        List<RevenueBucket> revenueSeries
) {
    public record RevenueBucket(
            String key,
            BigDecimal revenue,
            long paidBookings
    ) {
    }
}
