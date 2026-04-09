package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.entity.Refund;
import com.eaglepoint.workforce.enums.PaymentChannel;
import com.eaglepoint.workforce.enums.PaymentStatus;
import com.eaglepoint.workforce.repository.PaymentTransactionRepository;
import com.eaglepoint.workforce.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentTransactionRepository paymentRepo;
    private final RefundRepository refundRepo;

    public PaymentService(PaymentTransactionRepository paymentRepo, RefundRepository refundRepo) {
        this.paymentRepo = paymentRepo;
        this.refundRepo = refundRepo;
    }

    @Transactional
    public PaymentTransaction recordPayment(String idempotencyKey, String referenceNumber,
                                             BigDecimal amount, PaymentChannel channel,
                                             String location, String payerName, String description,
                                             String checkNumber, String cardLastFour,
                                             Long recordedBy, String recordedByUsername) {
        Optional<PaymentTransaction> existing = paymentRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        PaymentTransaction payment = new PaymentTransaction();
        payment.setIdempotencyKey(idempotencyKey);
        payment.setReferenceNumber(referenceNumber);
        payment.setAmount(amount);
        payment.setChannel(channel);
        payment.setLocation(location);
        payment.setPayerName(payerName);
        payment.setDescription(description);
        payment.setCheckNumber(checkNumber);
        payment.setCardLastFour(cardLastFour);
        payment.setRecordedBy(recordedBy);
        payment.setRecordedByUsername(recordedByUsername);
        payment.setTransactionDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.RECORDED);

        return paymentRepo.save(payment);
    }

    @Transactional
    public Refund processRefund(Long paymentId, BigDecimal refundAmount, String reason,
                                 Long processedBy, String processedByUsername) {
        PaymentTransaction payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        BigDecimal maxRefundable = payment.getAmount().subtract(payment.getRefundedAmount());
        if (refundAmount.compareTo(maxRefundable) > 0) {
            throw new RuntimeException("Refund amount " + refundAmount +
                    " exceeds available amount " + maxRefundable);
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(refundAmount);
        refund.setReason(reason);
        refund.setProcessedBy(processedBy);
        refund.setProcessedByUsername(processedByUsername);
        refund = refundRepo.save(refund);

        payment.setRefundedAmount(payment.getRefundedAmount().add(refundAmount));
        if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        paymentRepo.save(payment);

        return refund;
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> findAll() {
        return paymentRepo.findAllByOrderByTransactionDateDesc();
    }

    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> findById(Long id) {
        return paymentRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Refund> getRefundsForPayment(Long paymentId) {
        return refundRepo.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> findByLocationAndDateRange(String location,
                                                                LocalDateTime from, LocalDateTime to) {
        return paymentRepo.findByLocationAndTransactionDateBetween(location, from, to);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctLocations() {
        return paymentRepo.findDistinctLocations();
    }

    /**
     * Deterministic idempotency key from business fields only (no timestamp).
     * Same reference + channel + amount always produces the same key.
     */
    public static String generateIdempotencyKey(String referenceNumber, String channel, String amount) {
        try {
            String input = referenceNumber + "|" + channel + "|" + amount;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate idempotency key", e);
        }
    }
}
