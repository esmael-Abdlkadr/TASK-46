package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_entries", indexes = {
    @Index(name = "idx_bank_ref", columnList = "bank_reference"),
    @Index(name = "idx_bank_date", columnList = "entry_date"),
    @Index(name = "idx_bank_import", columnList = "import_id")
})
public class BankEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", nullable = false)
    private BankFileImport bankFileImport;

    @Column(name = "bank_reference", nullable = false, length = 50)
    private String bankReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(length = 500)
    private String description;

    @Column(name = "matched_payment_id")
    private Long matchedPaymentId;

    @Column(nullable = false)
    private boolean matched = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BankFileImport getBankFileImport() { return bankFileImport; }
    public void setBankFileImport(BankFileImport bankFileImport) { this.bankFileImport = bankFileImport; }
    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getMatchedPaymentId() { return matchedPaymentId; }
    public void setMatchedPaymentId(Long matchedPaymentId) { this.matchedPaymentId = matchedPaymentId; }
    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
