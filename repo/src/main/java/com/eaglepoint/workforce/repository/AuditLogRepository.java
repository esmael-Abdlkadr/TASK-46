package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.AuditLog;
import com.eaglepoint.workforce.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByAction(AuditAction action);
    List<AuditLog> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
