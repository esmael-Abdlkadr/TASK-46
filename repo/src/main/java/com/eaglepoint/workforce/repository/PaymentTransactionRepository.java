package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<PaymentTransaction> findByReferenceNumber(String referenceNumber);

    List<PaymentTransaction> findByStatus(PaymentStatus status);

    List<PaymentTransaction> findByLocationAndTransactionDateBetween(
            String location, LocalDateTime from, LocalDateTime to);

    @Query("SELECT p FROM PaymentTransaction p WHERE p.transactionDate BETWEEN :from AND :to " +
           "ORDER BY p.transactionDate DESC")
    List<PaymentTransaction> findByDateRange(@Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    @Query("SELECT p FROM PaymentTransaction p WHERE p.status = 'RECORDED' " +
           "AND p.referenceNumber = :ref")
    List<PaymentTransaction> findUnreconciledByReference(@Param("ref") String ref);

    @Query("SELECT DISTINCT p.location FROM PaymentTransaction p WHERE p.location IS NOT NULL " +
           "ORDER BY p.location")
    List<String> findDistinctLocations();

    List<PaymentTransaction> findAllByOrderByTransactionDateDescIdDesc();
}
