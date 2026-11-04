package com.ttcsn.payment.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Lịch sử giao dịch thanh toán.
 *
 * <p>Mỗi lần tạo QR sẽ có một bản ghi PENDING, webhook SePay cập nhật bản ghi đó
 * thành SUCCESS/FAILED. {@code providerTxnId} là ID giao dịch phía SePay, có unique
 * constraint để chống xử lý trùng khi webhook bị retry.
 *
 * <p>booking_id là ID tham chiếu sang booking-service, không đặt FK để giữ ranh giới
 * service. payment-service tự xác thực booking qua internal API trước khi ghi nhận.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(nullable = false, columnDefinition = "text")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PaymentStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String provider;

    @Column(name = "provider_txn_id", columnDefinition = "text")
    private String providerTxnId;

    @Column(name = "reference_code", columnDefinition = "text")
    private String referenceCode;

    @Column(name = "qr_url", columnDefinition = "text")
    private String qrUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private JsonNode rawPayload;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
