package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.enums.PaymentChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CreatePaymentRequest {
    private String idempotencyKey;
    @NotBlank(message = "Reference number is required")
    private String referenceNumber;
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    @NotNull(message = "Channel is required")
    private PaymentChannel channel;
    private String location;
    private String payerName;
    private String description;
    private String checkNumber;
    private String cardLastFour;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String k) { this.idempotencyKey = k; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String r) { this.referenceNumber = r; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public PaymentChannel getChannel() { return channel; }
    public void setChannel(PaymentChannel c) { this.channel = c; }
    public String getLocation() { return location; }
    public void setLocation(String l) { this.location = l; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String p) { this.payerName = p; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getCheckNumber() { return checkNumber; }
    public void setCheckNumber(String c) { this.checkNumber = c; }
    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String c) { this.cardLastFour = c; }
}
