package com.ttcsn.common.dto;

/**
 * SOA Step 0 — Hộp thư chung cho mọi service.
 * VOI gọi — Dùng chung cho mọi service. Tất cả REST API (trả về Response) đều bọc body dạng này qua api-gateway.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        long timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null, System.currentTimeMillis());
    }
}
