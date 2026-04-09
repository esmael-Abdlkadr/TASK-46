package com.eaglepoint.workforce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class RefundRequest {
    @NotNull(message = "Refund amount is required") @Positive(message = "Refund amount must be positive")
    private BigDecimal amount;
    @NotBlank(message = "Reason is required")
    private String reason;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
}
