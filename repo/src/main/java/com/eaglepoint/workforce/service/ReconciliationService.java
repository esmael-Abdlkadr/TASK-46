package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.BankEntry;
import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.entity.ReconciliationException;
import com.eaglepoint.workforce.enums.PaymentStatus;
import com.eaglepoint.workforce.enums.ReconciliationStatus;
import com.eaglepoint.workforce.repository.BankEntryRepository;
import com.eaglepoint.workforce.repository.PaymentTransactionRepository;
import com.eaglepoint.workforce.repository.ReconciliationExceptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationService {

    private final PaymentTransactionRepository paymentRepo;
    private final BankEntryRepository bankEntryRepo;
    private final ReconciliationExceptionRepository exceptionRepo;

    public ReconciliationService(PaymentTransactionRepository paymentRepo,
                                  BankEntryRepository bankEntryRepo,
                                  ReconciliationExceptionRepository exceptionRepo) {
        this.paymentRepo = paymentRepo;
        this.bankEntryRepo = bankEntryRepo;
        this.exceptionRepo = exceptionRepo;
    }

    @Transactional
    public int runReconciliation(Long bankImportId) {
        List<BankEntry> entries = bankEntryRepo.findByBankFileImportId(bankImportId);
        int matchedCount = 0;

        for (BankEntry entry : entries) {
            if (entry.isMatched()) continue;

            List<PaymentTransaction> matches = paymentRepo.findUnreconciledByReference(
                    entry.getBankReference());

            if (matches.isEmpty()) {
                createException(ReconciliationStatus.UNMATCHED_BANK, null, entry,
                        "No matching payment found for bank reference: " + entry.getBankReference());
                continue;
            }

            PaymentTransaction payment = matches.get(0);

            if (payment.getAmount().compareTo(entry.getAmount()) == 0) {
                entry.setMatched(true);
                entry.setMatchedPaymentId(payment.getId());
                bankEntryRepo.save(entry);

                payment.setStatus(PaymentStatus.RECONCILED);
                paymentRepo.save(payment);
                matchedCount++;
            } else {
                BigDecimal discrepancy = payment.getAmount().subtract(entry.getAmount());
                ReconciliationException exc = new ReconciliationException();
                exc.setStatus(ReconciliationStatus.DISCREPANCY);
                exc.setPaymentId(payment.getId());
                exc.setBankEntryId(entry.getId());
                exc.setPaymentAmount(payment.getAmount());
                exc.setBankAmount(entry.getAmount());
                exc.setDiscrepancyAmount(discrepancy);
                exc.setDescription("Amount mismatch: payment=" + payment.getAmount()
                        + " bank=" + entry.getAmount() + " diff=" + discrepancy);
                exceptionRepo.save(exc);
            }
        }

        flagUnmatchedPayments();

        return matchedCount;
    }

    private void flagUnmatchedPayments() {
        List<PaymentTransaction> recorded = paymentRepo.findByStatus(PaymentStatus.RECORDED);
        for (PaymentTransaction p : recorded) {
            List<BankEntry> bankMatches = bankEntryRepo.findByBankReference(p.getReferenceNumber());
            if (bankMatches.isEmpty()) {
                boolean alreadyFlagged = exceptionRepo.findByStatusNot(ReconciliationStatus.RESOLVED)
                        .stream().anyMatch(e -> p.getId().equals(e.getPaymentId()));
                if (!alreadyFlagged) {
                    createException(ReconciliationStatus.UNMATCHED_PAYMENT, p, null,
                            "Payment has no matching bank entry: ref=" + p.getReferenceNumber());
                }
            }
        }
    }

    private void createException(ReconciliationStatus status, PaymentTransaction payment,
                                  BankEntry bankEntry, String description) {
        ReconciliationException exc = new ReconciliationException();
        exc.setStatus(status);
        if (payment != null) {
            exc.setPaymentId(payment.getId());
            exc.setPaymentAmount(payment.getAmount());
        }
        if (bankEntry != null) {
            exc.setBankEntryId(bankEntry.getId());
            exc.setBankAmount(bankEntry.getAmount());
        }
        exc.setDescription(description);
        exceptionRepo.save(exc);
    }

    @Transactional
    public ReconciliationException resolveException(Long exceptionId, String resolutionNotes,
                                                      String resolvedBy) {
        ReconciliationException exc = exceptionRepo.findById(exceptionId)
                .orElseThrow(() -> new RuntimeException("Exception not found"));
        exc.setStatus(ReconciliationStatus.RESOLVED);
        exc.setResolutionNotes(resolutionNotes);
        exc.setResolvedBy(resolvedBy);
        exc.setResolvedAt(LocalDateTime.now());
        return exceptionRepo.save(exc);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationException> findAllExceptions() {
        return exceptionRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationException> findOpenExceptions() {
        return exceptionRepo.findByStatusNot(ReconciliationStatus.RESOLVED);
    }

    @Transactional(readOnly = true)
    public long countOpenExceptions() {
        return exceptionRepo.countByStatusNot(ReconciliationStatus.RESOLVED);
    }

    @Transactional(readOnly = true)
    public Optional<ReconciliationException> findExceptionById(Long id) {
        return exceptionRepo.findById(id);
    }
}
