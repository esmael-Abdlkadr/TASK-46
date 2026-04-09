package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.DispatchOfferLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchOfferLogRepository extends JpaRepository<DispatchOfferLog, Long> {

    List<DispatchOfferLog> findByAssignmentIdOrderByOfferedAtDesc(Long assignmentId);

    List<DispatchOfferLog> findByCollectorId(Long collectorId);
}
