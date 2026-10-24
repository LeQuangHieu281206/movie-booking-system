package com.ttcsn.payment.dto.request;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) dùng để nhận dữ liệu từ Webhook của SePay.
 * Cấu trúc ánh xạ (map) chính xác với định dạng JSON mà SePay trả về khi có giao dịch phát sinh.
 */
@Data
public class SePayWebhookDto {
    private Long id;
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String subAccount;
    private BigDecimal transferAmount;
    
    // Thuộc tính phân loại giao dịch: "in" (nhận tiền) hoặc "out" (chuyển tiền)
    private String transferType; 
    
    // Nội dung chuyển khoản, chứa mã đối soát giao dịch (VD: DH123)
    private String content;      
    private String referenceCode;
    private String description;
}