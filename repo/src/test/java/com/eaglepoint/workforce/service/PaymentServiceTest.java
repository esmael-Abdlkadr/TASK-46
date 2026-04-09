package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.entity.Refund;
import com.eaglepoint.workforce.enums.PaymentChannel;
import com.eaglepoint.workforce.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    void recordPayment_success() {
        PaymentTransaction p = paymentService.recordPayment(
                "idem-key-001", "REF-001", new BigDecimal("150.00"),
                PaymentChannel.CASH, "Main Office", "John Doe", "Monthly fee",
                null, null, 1L, "admin");

        assertNotNull(p.getId());
        assertEquals("REF-001", p.getReferenceNumber());
        assertEquals(new BigDecimal("150.00"), p.getAmount());
        assertEquals(PaymentStatus.RECORDED, p.getStatus());
    }

    @Test
    void idempotentPayment_returnsSameRecord() {
        PaymentTransaction p1 = paymentService.recordPayment(
                "idem-dup-001", "REF-DUP", new BigDecimal("100.00"),
                PaymentChannel.CHECK, "Office A", "Jane", null, "CHK-100", null, 1L, "admin");

        PaymentTransaction p2 = paymentService.recordPayment(
                "idem-dup-001", "REF-DUP", new BigDecimal("100.00"),
                PaymentChannel.CHECK, "Office A", "Jane", null, "CHK-100", null, 1L, "admin");

        assertEquals(p1.getId(), p2.getId());
    }

    @Test
    void rejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.recordPayment("neg-001", "REF-NEG",
                        new BigDecimal("-50.00"), PaymentChannel.CASH, null, null, null,
                        null, null, 1L, "admin"));
    }

    @Test
    void fullRefund() {
        PaymentTransaction p = paymentService.recordPayment(
                "refund-full-001", "REF-RF1", new BigDecimal("200.00"),
                PaymentChannel.CASH, null, null, null, null, null, 1L, "admin");

        Refund refund = paymentService.processRefund(p.getId(),
                new BigDecimal("200.00"), "Customer request", 1L, "admin");

        assertNotNull(refund.getId());
        assertEquals(new BigDecimal("200.00"), refund.getAmount());

        PaymentTransaction updated = paymentService.findById(p.getId()).orElseThrow();
        assertEquals(PaymentStatus.REFUNDED, updated.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), updated.getNetAmount().setScale(2));
    }

    @Test
    void partialRefund() {
        PaymentTransaction p = paymentService.recordPayment(
                "refund-part-001", "REF-RF2", new BigDecimal("300.00"),
                PaymentChannel.MANUAL_CARD, null, null, null, null, "1234", 1L, "admin");

        paymentService.processRefund(p.getId(), new BigDecimal("100.00"), "Partial return", 1L, "admin");

        PaymentTransaction updated = paymentService.findById(p.getId()).orElseThrow();
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, updated.getStatus());
        assertEquals(new BigDecimal("200.00"), updated.getNetAmount());
    }

    @Test
    void refundExceedingBalanceRejected() {
        PaymentTransaction p = paymentService.recordPayment(
                "refund-over-001", "REF-RF3", new BigDecimal("100.00"),
                PaymentChannel.CASH, null, null, null, null, null, 1L, "admin");

        assertThrows(RuntimeException.class, () ->
                paymentService.processRefund(p.getId(), new BigDecimal("150.00"),
                        "Over-refund", 1L, "admin"));
    }

    @Test
    void multiplePartialRefunds() {
        PaymentTransaction p = paymentService.recordPayment(
                "refund-multi-001", "REF-RF4", new BigDecimal("500.00"),
                PaymentChannel.CHECK, null, null, null, "CHK-500", null, 1L, "admin");

        paymentService.processRefund(p.getId(), new BigDecimal("100.00"), "First", 1L, "admin");
        paymentService.processRefund(p.getId(), new BigDecimal("200.00"), "Second", 1L, "admin");

        PaymentTransaction updated = paymentService.findById(p.getId()).orElseThrow();
        assertEquals(new BigDecimal("300.00"), updated.getRefundedAmount());
        assertEquals(new BigDecimal("200.00"), updated.getNetAmount());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, updated.getStatus());

        var refunds = paymentService.getRefundsForPayment(p.getId());
        assertEquals(2, refunds.size());
    }

    @Test
    void idempotencyKeyGeneration_deterministic() {
        String key1 = PaymentService.generateIdempotencyKey("REF-1", "CASH", "100.00");
        String key2 = PaymentService.generateIdempotencyKey("REF-1", "CASH", "100.00");
        String key3 = PaymentService.generateIdempotencyKey("REF-2", "CASH", "100.00");

        assertEquals(key1, key2, "Same business fields must produce same key");
        assertNotEquals(key1, key3);
        assertEquals(64, key1.length());
    }
}
