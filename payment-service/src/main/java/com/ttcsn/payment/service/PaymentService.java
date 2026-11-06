package com.ttcsn.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttcsn.common.dto.BookingPaymentView;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.common.event.Topics;
import com.ttcsn.common.exception.ConflictException;
import com.ttcsn.common.exception.NotFoundException;
import com.ttcsn.payment.client.BookingClient;
import com.ttcsn.payment.config.RabbitMQConfig;
import com.ttcsn.payment.config.SepayProperties;
import com.ttcsn.payment.dto.request.SePayWebhookDto;
import com.ttcsn.payment.entity.Payment;
import com.ttcsn.payment.entity.PaymentStatus;
import com.ttcsn.payment.repository.PaymentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service xử lý nghiệp vụ thanh toán tích hợp cổng SePay (mô hình SOA).
 *
 * <p>Mỗi booking có thể có nhiều payment attempt, nhưng chỉ một payment PENDING
 * được tái sử dụng khi tạo lại QR. Webhook SePay cập nhật payment PENDING đó thành
 * SUCCESS/FAILED và lưu {@code provider_txn_id} để chống xử lý trùng.
 *
 * <p>An toàn dữ liệu: bookingId duy nhất client được phép gửi; userId lấy từ X-User-Id
 * (gateway gắn sau khi verify JWT) và amount luôn đọc từ booking ở server qua
 * {@link BookingClient}. Webhook còn đối soát số tiền nhận được với total_amount
 * trước khi publish event.
 *
 * @author quanghieu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PROVIDER_SEPAY = "SEPAY";
    private static final String BOOKING_PREFIX = "DH";
    private static final Pattern BOOKING_REFERENCE =
            Pattern.compile(Pattern.quote(BOOKING_PREFIX) + "(\\d+)");

    private final BookingClient bookingClient;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final SepayProperties sepayProperties;

    /**
     * Xác thực booking, ghi nhận payment PENDING rồi sinh URL ảnh VietQR.
     */
    @Transactional
    public String createSePayPayment(Long bookingId, Long userId) {
        if (bookingId == null) {
            throw new IllegalArgumentException("bookingId không được để trống");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Thiếu X-User-Id");
        }

        BookingPaymentView booking = loadOwnedPendingBooking(bookingId, userId);
        BigDecimal amount = booking.totalAmount();
        String referenceCode = referenceCode(bookingId);
        String qrUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s",
                sepayProperties.accountNumber(), sepayProperties.bankName(),
                amount.toPlainString(), referenceCode);

        paymentRepository.upsertPendingPayment(bookingId, amount, referenceCode, qrUrl);
        Long paymentId = paymentRepository
                .findFirstByBookingIdAndStatusOrderByPaymentIdDesc(bookingId, PaymentStatus.PENDING)
                .map(Payment::getPaymentId)
                .orElse(null);

        log.info("[payment-service] Đã tạo QR cho bookingId={}, paymentId={}, amount={}",
                bookingId, paymentId, amount);
        return qrUrl;
    }

    /**
     * Xử lý webhook SePay.
     *
     * <p>Idempotent theo {@code provider_txn_id}: webhook retry với cùng ID giao dịch
     * sẽ bị bỏ qua. Chỉ publish event SUCCESS khi booking tồn tại, đang PENDING và
     * số tiền nhận được khớp đúng total_amount.
     */
    @Transactional
    public void processSePayWebhook(SePayWebhookDto request) {
        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            return;
        }

        String content = request.getContent();
        if (content == null) {
            return;
        }

        Long bookingId = extractBookingId(content.toUpperCase());
        if (bookingId == null) {
            log.warn("[payment-service] Không tìm thấy mã đối soát (prefix {}) trong nội dung: {}",
                    BOOKING_PREFIX, content);
            return;
        }

        String providerTxnId = request.getId() == null ? null : String.valueOf(request.getId());
        if (providerTxnId != null
                && paymentRepository.existsByProviderAndProviderTxnId(PROVIDER_SEPAY, providerTxnId)) {
            log.info("[payment-service] Bỏ qua webhook trùng, providerTxnId={}, bookingId={}",
                    providerTxnId, bookingId);
            return;
        }

        BookingPaymentView booking;
        try {
            booking = bookingClient.getBooking(bookingId);
        } catch (FeignException.NotFound ex) {
            // payments.booking_id có FK -> bookings, nên không ghi payment cho booking không tồn tại.
            log.warn("[payment-service] Bỏ qua webhook vì booking {} không tồn tại, providerTxnId={}",
                    bookingId, providerTxnId);
            return;
        } catch (FeignException ex) {
            throw new IllegalStateException(
                    "Không đọc được booking " + bookingId + " để đối soát thanh toán", ex);
        }

        if (!"PENDING".equalsIgnoreCase(booking.status())) {
            recordFailed(bookingId, request, providerTxnId,
                    "Booking đang ở trạng thái " + booking.status() + ", không nhận thanh toán");
            return;
        }

        BigDecimal expectedAmount = booking.totalAmount();
        BigDecimal receivedAmount = request.getTransferAmount();
        if (expectedAmount == null || receivedAmount == null
                || expectedAmount.compareTo(receivedAmount) != 0) {
            recordFailed(bookingId, request, providerTxnId,
                    "Số tiền không khớp: cần " + expectedAmount + ", nhận " + receivedAmount);
            return;
        }

        String referenceCode = referenceCode(bookingId);
        String rawPayload = rawPayload(request);
        OffsetDateTime paidAt = OffsetDateTime.now();
        Long savedPaymentId;

        // Khóa payment PENDING của booking để hai webhook đồng thời không cùng publish.
        // Nếu không có dòng PENDING, insert atomic + ON CONFLICT để chỉ một transaction thắng.
        Optional<Payment> pending = paymentRepository
                .findFirstByBookingIdAndStatusOrderByPaymentIdDesc(bookingId, PaymentStatus.PENDING);
        if (pending.isPresent()) {
            Payment payment = pending.get();
            payment.setAmount(receivedAmount);
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setProviderTxnId(providerTxnId);
            payment.setReferenceCode(referenceCode);
            payment.setRawPayload(objectMapper.valueToTree(request));
            payment.setPaidAt(paidAt);
            payment.setFailureReason(null);
            savedPaymentId = paymentRepository.save(payment).getPaymentId();
        } else {
            int inserted = paymentRepository.insertSuccessIfAbsent(
                    bookingId, receivedAmount, providerTxnId, referenceCode, rawPayload, paidAt);
            if (inserted == 0) {
                log.info("[payment-service] Webhook đồng thời đã được xử lý, bỏ qua publish trùng. bookingId={}, providerTxnId={}",
                        bookingId, providerTxnId);
                return;
            }
            savedPaymentId = null;
        }

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                bookingId,
                booking.userId(),
                receivedAmount,
                paidAt,
                PaymentCompletedEvent.STATUS_SUCCESS
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, Topics.PAYMENT_COMPLETED, event);

        log.info("[payment-service] Đã xác nhận thanh toán bookingId={}, paymentId={}, providerTxnId={}, amount={}",
                bookingId, savedPaymentId, providerTxnId, receivedAmount);
    }

    private void recordFailed(Long bookingId, SePayWebhookDto request, String providerTxnId, String reason) {
        int inserted = paymentRepository.insertFailedIfAbsent(
                bookingId,
                request.getTransferAmount() == null ? BigDecimal.ZERO : request.getTransferAmount(),
                providerTxnId,
                referenceCode(bookingId),
                rawPayload(request),
                reason);
        if (inserted > 0) {
            log.warn("[payment-service] Ghi nhận payment FAILED cho bookingId={}, lý do: {}", bookingId, reason);
        } else {
            log.info("[payment-service] Payment FAILED đã tồn tại, bỏ qua. bookingId={}, providerTxnId={}",
                    bookingId, providerTxnId);
        }
    }

    /**
     * Đọc booking qua internal API và kiểm tra: tồn tại, đúng chủ sở hữu, đang PENDING,
     * có số tiền hợp lệ. Booking không tồn tại hoặc không thuộc user → 404; sai trạng thái → 409.
     */
    private BookingPaymentView loadOwnedPendingBooking(Long bookingId, Long userId) {
        BookingPaymentView booking;
        try {
            booking = bookingClient.getBooking(bookingId);
        } catch (FeignException.NotFound ex) {
            throw new NotFoundException("Không tìm thấy đơn đặt vé với bookingId = " + bookingId);
        }
        if (booking == null || !Objects.equals(booking.userId(), userId)) {
            throw new NotFoundException("Không tìm thấy đơn đặt vé với bookingId = " + bookingId);
        }
        if (!"PENDING".equals(booking.status())) {
            throw new ConflictException("Đơn đặt vé không ở trạng thái chờ thanh toán");
        }
        if (booking.totalAmount() == null || booking.totalAmount().signum() <= 0) {
            throw new ConflictException("Đơn đặt vé chưa có số tiền hợp lệ");
        }
        return booking;
    }

    private String referenceCode(Long bookingId) {
        return BOOKING_PREFIX + bookingId;
    }

    private String rawPayload(SePayWebhookDto request) {
        return objectMapper.valueToTree(request).toString();
    }

    private Long extractBookingId(String content) {
        Matcher matcher = BOOKING_REFERENCE.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            log.error("[payment-service] Mã booking trong nội dung webhook không phải số hợp lệ: {}", content);
            return null;
        }
    }
}
