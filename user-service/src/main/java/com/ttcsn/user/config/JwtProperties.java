package com.ttcsn.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình JWT thuộc về user-service.
 *
 * Map các key trong application.properties:
 *   jwt.secret                ← biến môi trường JWT_SECRET (bắt buộc, >= 32 ký tự)
 *   jwt.access-expiration-ms  ← mặc định 7200000  (2 giờ)
 *   jwt.refresh-expiration-ms ← mặc định 604800000 (7 ngày)
 *   jwt.issuer                ← mặc định mtbs-user-service
 *
 * Fail-fast lúc khởi động: secret rỗng hoặc ngắn hơn 32 ký tự → dừng ngay
 * với thông báo rõ ràng (xem {@link #afterPropertiesSet()}).
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Long accessExpirationMs,
        Long refreshExpirationMs,
        String issuer
) implements org.springframework.beans.factory.InitializingBean {

    /** Độ dài tối thiểu của secret để HS256 an toàn theo yêu cầu. */
    public static final int MIN_SECRET_LENGTH = 32;

    @Override
    public void afterPropertiesSet() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Cấu hình JWT không hợp lệ: 'jwt.secret' trống. "
                            + "Hãy đặt biến môi trường JWT_SECRET (tối thiểu " + MIN_SECRET_LENGTH
                            + " ký tự), ví dụ: openssl rand -base64 48");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "Cấu hình JWT không hợp lệ: 'jwt.secret' phải dài tối thiểu "
                            + MIN_SECRET_LENGTH + " ký tự (hiện tại: " + secret.length() + ").");
        }
        if (accessExpirationMs == null || accessExpirationMs <= 0) {
            throw new IllegalStateException("Cấu hình JWT không hợp lệ: 'jwt.access-expiration-ms' phải > 0.");
        }
        if (refreshExpirationMs == null || refreshExpirationMs <= 0) {
            throw new IllegalStateException("Cấu hình JWT không hợp lệ: 'jwt.refresh-expiration-ms' phải > 0.");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("Cấu hình JWT không hợp lệ: 'jwt.issuer' không được để trống.");
        }
    }
}
