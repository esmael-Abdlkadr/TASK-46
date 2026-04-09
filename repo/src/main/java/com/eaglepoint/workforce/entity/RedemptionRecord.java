package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemption_records", indexes = {
    @Index(name = "idx_redemption_member", columnList = "member_id")
})
public class RedemptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "redemption_code", nullable = false, unique = true, length = 30)
    private String redemptionCode;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "item_description", nullable = false, length = 300)
    private String itemDescription;

    @Column(name = "points_redeemed")
    private Integer pointsRedeemed;

    @Column(name = "redemption_value", precision = 12, scale = 2)
    private BigDecimal value;

    @Column(nullable = false, length = 30)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRedemptionCode() { return redemptionCode; }
    public void setRedemptionCode(String redemptionCode) { this.redemptionCode = redemptionCode; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getItemDescription() { return itemDescription; }
    public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
    public Integer getPointsRedeemed() { return pointsRedeemed; }
    public void setPointsRedeemed(Integer pointsRedeemed) { this.pointsRedeemed = pointsRedeemed; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
