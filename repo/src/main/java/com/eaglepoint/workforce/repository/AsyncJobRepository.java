package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.AsyncJob;
import com.eaglepoint.workforce.enums.AsyncJobStatus;
import com.eaglepoint.workforce.enums.AsyncJobType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, Long> {
    List<AsyncJob> findByStatusOrderByCreatedAtAsc(AsyncJobStatus status);
    List<AsyncJob> findByJobTypeAndStatusIn(AsyncJobType type, List<AsyncJobStatus> statuses);
    List<AsyncJob> findAllByOrderByCreatedAtDesc();
    long countByStatus(AsyncJobStatus status);
    long countByJobTypeAndStatus(AsyncJobType type, AsyncJobStatus status);
}
