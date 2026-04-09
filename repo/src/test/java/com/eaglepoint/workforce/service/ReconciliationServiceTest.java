package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.PaymentChannel;
import com.eaglepoint.workforce.enums.PaymentStatus;
import com.eaglepoint.workforce.enums.ReconciliationStatus;
import com.eaglepoint.workforce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReconciliationServiceTest {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private PaymentTransactionRepository paymentRepo;

    @Autowired
    private BankFileImportRepository bankImportRepo;

    @Autowired
    private BankEntryRepository bankEntryRepo;

    private BankFileImport bankImport;

    @BeforeEach
    void setUp() {
        bankImport = new BankFileImport();
        bankImport.setFileName("test-bank.csv");
        bankImport.setFileHash("test-hash-" + System.nanoTime());
        bankImport.setTotalEntries(0);
        bankImport.setImportedBy(1L);
        bankImport = bankImportRepo.save(bankImport);
    }

    @Test
    void matchingPaymentAndBankEntry() {
        createPayment("MATCH-001", new BigDecimal("250.00"));
        createBankEntry("MATCH-001", new BigDecimal("250.00"));

        int matched = reconciliationService.runReconciliation(bankImport.getId());

        assertEquals(1, matched);
        PaymentTransaction p = paymentRepo.findByReferenceNumber("MATCH-001").orElseThrow();
        assertEquals(PaymentStatus.RECONCILED, p.getStatus());
    }

    @Test
    void amountDiscrepancyCreatesException() {
        createPayment("DISC-001", new BigDecimal("100.00"));
        createBankEntry("DISC-001", new BigDecimal("95.00"));

        reconciliationService.runReconciliation(bankImport.getId());

        List<ReconciliationException> exceptions = reconciliationService.findOpenExceptions();
        assertTrue(exceptions.stream().anyMatch(e ->
                e.getStatus() == ReconciliationStatus.DISCREPANCY));
    }

    @Test
    void unmatchedBankEntryCreatesException() {
        createBankEntry("NOPMATCH-001", new BigDecimal("75.00"));

        reconciliationService.runReconciliation(bankImport.getId());

        List<ReconciliationException> exceptions = reconciliationService.findOpenExceptions();
        assertTrue(exceptions.stream().anyMatch(e ->
                e.getStatus() == ReconciliationStatus.UNMATCHED_BANK));
    }

    @Test
    void resolveException() {
        createBankEntry("RESOLVE-001", new BigDecimal("50.00"));
        reconciliationService.runReconciliation(bankImport.getId());

        List<ReconciliationException> open = reconciliationService.findOpenExceptions();
        assertFalse(open.isEmpty());

        ReconciliationException exc = open.get(0);
        reconciliationService.resolveException(exc.getId(), "Manually verified", "admin");

        ReconciliationException resolved = reconciliationService.findExceptionById(exc.getId()).orElseThrow();
        assertEquals(ReconciliationStatus.RESOLVED, resolved.getStatus());
        assertEquals("admin", resolved.getResolvedBy());
        assertNotNull(resolved.getResolvedAt());
    }

    private void createPayment(String ref, BigDecimal amount) {
        PaymentTransaction p = new PaymentTransaction();
        p.setIdempotencyKey("idem-" + ref + "-" + System.nanoTime());
        p.setReferenceNumber(ref);
        p.setAmount(amount);
        p.setRefundedAmount(BigDecimal.ZERO);
        p.setChannel(PaymentChannel.CASH);
        p.setStatus(PaymentStatus.RECORDED);
        p.setRecordedBy(1L);
        p.setTransactionDate(LocalDateTime.now());
        paymentRepo.save(p);
    }

    private void createBankEntry(String ref, BigDecimal amount) {
        BankEntry entry = new BankEntry();
        entry.setBankFileImport(bankImport);
        entry.setBankReference(ref);
        entry.setAmount(amount);
        entry.setEntryDate(LocalDate.now());
        bankEntryRepo.save(entry);
    }
}
