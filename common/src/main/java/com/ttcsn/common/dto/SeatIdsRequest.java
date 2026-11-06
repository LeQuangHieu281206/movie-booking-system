package com.ttcsn.common.dto;

import java.util.List;

/**
 *
 * @author quanghieu
 */
public record SeatIdsRequest(
        Long showtimeId,
        List<Long> seatIds,
        String holdBy
) {
    /** Danh sách ghế hợp lệ để xử lý; rỗng thì từ chối ngay tại biên. */
    public List<Long> normalizedSeatIds() {
        return SeatIds.normalize(seatIds);
    }
}
