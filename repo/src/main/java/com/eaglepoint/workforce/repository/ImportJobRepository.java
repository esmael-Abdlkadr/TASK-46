package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
    boolean existsByFileFingerprint(String fingerprint);
    List<ImportJob> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    List<ImportJob> findAllByOrderByCreatedAtDesc();
    Optional<ImportJob> findByIdAndCreatedBy(Long id, Long createdBy);
}
