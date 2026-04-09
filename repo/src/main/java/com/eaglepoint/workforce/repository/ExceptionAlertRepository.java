package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.ExceptionAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExceptionAlertRepository extends JpaRepository<ExceptionAlert, Long> {
    List<ExceptionAlert> findByResolvedFalse();
    long countByResolvedFalse();
    List<ExceptionAlert> findTop10ByResolvedFalseOrderByCreatedAtDesc();
}
