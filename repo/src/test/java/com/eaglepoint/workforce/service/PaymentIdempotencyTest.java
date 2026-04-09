package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.enums.PaymentChannel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentIdempotencyTest {

    @Autowired private PaymentService paymentService;

    @Test
    void deterministicKey_noTimestamp() {
        String k1 = PaymentService.generateIdempotencyKey("REF-1", "CASH", "100.00");
        String k2 = PaymentService.generateIdempotencyKey("REF-1", "CASH", "100.00");
        assertEquals(k1, k2, "Same business fields must produce same key");
    }

    @Test
    void differentFields_differentKey() {
        String k1 = PaymentService.generateIdempotencyKey("REF-1", "CASH", "100.00");
        String k2 = PaymentService.generateIdempotencyKey("REF-2", "CASH", "100.00");
        assertNotEquals(k1, k2);
    }

    @Test
    void duplicateSubmission_returnsSameRecord() {
        String key = PaymentService.generateIdempotencyKey("DUP-001", "CHECK", "250.00");
        PaymentTransaction p1 = paymentService.recordPayment(key, "DUP-001",
                new java.math.BigDecimal("250.00"), PaymentChannel.CHECK,
                null, null, null, "CHK-1", null, 1L, "admin");

        PaymentTransaction p2 = paymentService.recordPayment(key, "DUP-001",
                new java.math.BigDecimal("250.00"), PaymentChannel.CHECK,
                null, null, null, "CHK-1", null, 1L, "admin");

        assertEquals(p1.getId(), p2.getId(), "Duplicate key must return existing record");
    }

    @Test
    void clientProvidedKey_works() {
        String clientKey = "client-custom-key-12345";
        PaymentTransaction p = paymentService.recordPayment(clientKey, "CLI-001",
                new java.math.BigDecimal("99.99"), PaymentChannel.CASH,
                null, null, null, null, null, 1L, "admin");

        assertEquals(clientKey, p.getIdempotencyKey());
    }
}
