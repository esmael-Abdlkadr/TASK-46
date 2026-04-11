package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.enums.PaymentChannel;
import com.eaglepoint.workforce.enums.PaymentStatus;
import com.eaglepoint.workforce.exception.ResourceNotFoundException;
import com.eaglepoint.workforce.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that PaymentService uses domain exceptions (not generic RuntimeException):
 * - ResourceNotFoundException for missing payment
 * - IllegalArgumentException for business rule violations
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentDomainExceptionTest {

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentTransactionRepository paymentRepo;

    @Test
    void refund_nonexistentPayment_throwsResourceNotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.processRefund(99999L, BigDecimal.TEN, "test", 1L, "user"));
    }

    @Test
    void refund_exceedingBalance_throwsIllegalArgument() {
        PaymentTransaction p = createPayment("100.00");
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processRefund(p.getId(), new BigDecimal("200.00"), "test", 1L, "user"));
    }

    @Test
    void refund_negativeAmount_throwsIllegalArgument() {
        PaymentTransaction p = createPayment("100.00");
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processRefund(p.getId(), new BigDecimal("-10.00"), "test", 1L, "user"));
    }

    @Test
    void refund_validAmount_succeeds() {
        PaymentTransaction p = createPayment("100.00");
        assertDoesNotThrow(() ->
                paymentService.processRefund(p.getId(), new BigDecimal("50.00"), "test", 1L, "user"));

        PaymentTransaction updated = paymentRepo.findById(p.getId()).orElseThrow();
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, updated.getStatus());
    }

    private PaymentTransaction createPayment(String amount) {
        return paymentService.recordPayment(
                "key-" + System.nanoTime(), "REF-001",
                new BigDecimal(amount), PaymentChannel.CASH,
                "loc", "payer", "desc", null, null, 1L, "user");
    }
}
