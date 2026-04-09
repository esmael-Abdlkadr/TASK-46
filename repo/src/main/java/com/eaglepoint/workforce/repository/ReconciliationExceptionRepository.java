package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.ReconciliationException;
import com.eaglepoint.workforce.enums.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationExceptionRepository extends JpaRepository<ReconciliationException, Long> {
    List<ReconciliationException> findByStatusNot(ReconciliationStatus status);
    List<ReconciliationException> findByStatus(ReconciliationStatus status);
    List<ReconciliationException> findAllByOrderByCreatedAtDesc();
    long countByStatusNot(ReconciliationStatus status);
}
