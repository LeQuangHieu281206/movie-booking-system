package com.ttcsn.common.dto;

import java.util.List;
import java.util.Objects;

/**
 * Quy tắc chuẩn hoá danh sách ghế dùng chung cho mọi request đặt/giữ ghế.
 */
public final class SeatIds {

    private SeatIds() {
    }

    /**
     * Bỏ null, bỏ trùng, giữ thứ tự xuất hiện. Danh sách rỗng bị từ chối
     * với thông điệp thống nhất (400 ở tầng API).
     */
    public static List<Long> normalize(List<Long> seatIds) {
        List<Long> ids = seatIds == null
                ? List.of()
                : seatIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Danh sách ghế đặt không được để trống");
        }
        return ids;
    }
}
