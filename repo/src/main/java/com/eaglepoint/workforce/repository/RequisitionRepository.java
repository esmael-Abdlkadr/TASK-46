package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.Requisition;
import com.eaglepoint.workforce.enums.RequisitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RequisitionRepository extends JpaRepository<Requisition, Long> {
    List<Requisition> findByStatus(RequisitionStatus status);
    long countByStatus(RequisitionStatus status);
    List<Requisition> findTop10ByStatusOrderByCreatedAtDesc(RequisitionStatus status);
}
