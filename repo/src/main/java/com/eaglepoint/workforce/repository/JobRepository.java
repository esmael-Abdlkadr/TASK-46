package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.Job;
import com.eaglepoint.workforce.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);
    long countByStatus(JobStatus status);
    List<Job> findTop10ByStatusOrderByCreatedAtDesc(JobStatus status);
}
