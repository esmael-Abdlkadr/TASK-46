package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.masking.MaskingUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Role-aware view of a payment. Non-admin sees masked sensitive fields.
 */
public class PaymentView {
    private final Long id;
    private final String referenceNumber;
    private final BigDecimal amount;
    private final BigDecimal refundedAmount;
    private final BigDecimal netAmount;
    private final String channel;
    private final String status;
    private final String location;
    private final String payerName;
    private final String checkNumber;
    private final String cardLastFour;
    private final String description;
    private final String recordedByUsername;
    private final String idempotencyKey;
    private final LocalDateTime transactionDate;

    public PaymentView(PaymentTransaction p, boolean isAdmin) {
        this.id = p.getId();
        this.referenceNumber = p.getReferenceNumber();
        this.amount = p.getAmount();
        this.refundedAmount = p.getRefundedAmount();
        this.netAmount = p.getNetAmount();
        this.channel = p.getChannel().getDisplayName();
        this.status = p.getStatus().getDisplayName();
        this.location = p.getLocation();
        this.payerName = MaskingUtil.mask(p.getPayerName(), isAdmin);
        this.checkNumber = MaskingUtil.mask(p.getCheckNumber(), isAdmin);
        this.cardLastFour = p.getCardLastFour(); // already last-4 only
        this.description = p.getDescription();
        this.recordedByUsername = p.getRecordedByUsername();
        this.idempotencyKey = p.getIdempotencyKey();
        this.transactionDate = p.getTransactionDate();
    }

    public Long getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }
    public String getPayerName() { return payerName; }
    public String getCheckNumber() { return checkNumber; }
    public String getCardLastFour() { return cardLastFour; }
    public String getDescription() { return description; }
    public String getRecordedByUsername() { return recordedByUsername; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
}
