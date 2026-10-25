package com.ttcsn.gateway.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Security configuration for the gateway.
 *
 * <p>Map from application.yml:
 * <pre>
 * gateway.security.jwt-secret      ← JWT_SECRET (bắt buộc, >= 32 ký tự)
 * gateway.security.jwt-issuer      ← issuer dùng chung với user-service
 * gateway.security.internal-token  ← INTERNAL_TOKEN (bắt buộc, >= 32 ký tự, gateway → service)
 * </pre>
 *
 * <p>Fail-fast lúc khởi động nếu thiếu secret/token, tránh chạy gateway ở trạng thái
 * không thể xác thực hoặc chuyển tiếp request nội bộ.
 */
@ConfigurationProperties(prefix = "gateway.security")
public record GatewaySecurityProperties(
        String jwtSecret,
        String jwtIssuer,
        String internalToken
) implements InitializingBean {

    /** Độ dài tối thiểu của secret HS256 (ký tự ASCII = byte). */
    public static final int MIN_SECRET_LENGTH = 32;

    /** Độ dài tối thiểu của token nội bộ gateway → service. */
    public static final int MIN_INTERNAL_TOKEN_LENGTH = 32;

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "Cấu hình JWT không hợp lệ: 'JWT_SECRET' trống. "
                            + "Đặt biến môi trường JWT_SECRET (tối thiểu " + MIN_SECRET_LENGTH
                            + " ký tự), ví dụ: openssl rand -base64 48");
        }
        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "Cấu hình JWT không hợp lệ: 'JWT_SECRET' phải dài tối thiểu "
                            + MIN_SECRET_LENGTH + " ký tự (hiện tại: " + jwtSecret.length() + ").");
        }
        if (jwtIssuer == null || jwtIssuer.isBlank()) {
            throw new IllegalStateException("Cấu hình JWT không hợp lệ: 'jwt-issuer' không được để trống.");
        }
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalStateException(
                    "Cấu hình nội bộ không hợp lệ: 'INTERNAL_TOKEN' trống. "
                            + "Đặt biến môi trường INTERNAL_TOKEN (tối thiểu "
                            + MIN_INTERNAL_TOKEN_LENGTH + " ký tự), ví dụ: openssl rand -hex 32");
        }
        if (internalToken.length() < MIN_INTERNAL_TOKEN_LENGTH) {
            throw new IllegalStateException(
                    "Cấu hình nội bộ không hợp lệ: 'INTERNAL_TOKEN' phải dài tối thiểu "
                            + MIN_INTERNAL_TOKEN_LENGTH + " ký tự (hiện tại: "
                            + internalToken.length() + ").");
        }
    }
}
