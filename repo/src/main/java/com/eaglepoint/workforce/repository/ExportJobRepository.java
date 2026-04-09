package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.ExportJob;
import com.eaglepoint.workforce.enums.ExportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {
    List<ExportJob> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    List<ExportJob> findByStatusOrderByCreatedAtAsc(ExportStatus status);
    List<ExportJob> findAllByOrderByCreatedAtDesc();
    Optional<ExportJob> findByIdAndCreatedBy(Long id, Long createdBy);
}
