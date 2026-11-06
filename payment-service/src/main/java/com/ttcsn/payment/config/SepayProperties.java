package com.ttcsn.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cổng SePay: tài khoản nhận tiền dựng URL VietQR và API key xác thực webhook.
 * Không hard-code trong service để đổi tài khoản/ngân hàng/key chỉ cần sửa cấu hình.
 *
 * @param bankName      mã ngân hàng cho qr.sepay.vn (ví dụ MBBank)
 * @param accountNumber số tài khoản nhận tiền
 * @param webhookApiKey key SePay gửi kèm webhook; rỗng = chế độ dev, chấp nhận mọi nguồn
 */
@ConfigurationProperties(prefix = "app.sepay")
public record SepayProperties(
        String bankName,
        String accountNumber,
        String webhookApiKey
) {

    public SepayProperties {
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("Cấu hình thiếu app.sepay.bank-name");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Cấu hình thiếu app.sepay.account-number");
        }
        webhookApiKey = webhookApiKey == null ? "" : webhookApiKey.trim();
    }
}
