package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.AsyncJob;
import com.eaglepoint.workforce.enums.AsyncJobStatus;
import com.eaglepoint.workforce.enums.AsyncJobType;
import com.eaglepoint.workforce.repository.AsyncJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class AsyncJobService {

    private static final Logger log = LoggerFactory.getLogger(AsyncJobService.class);

    @Value("${app.job-queue.max-queued:100}")
    private int maxQueued;

    @Value("${app.job-queue.max-running:4}")
    private int maxRunning;

    @Value("${app.job-queue.warn-threshold:50}")
    private int warnThreshold;

    private final AsyncJobRepository jobRepository;

    public AsyncJobService(AsyncJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public AsyncJob submit(AsyncJobType type, String description, String inputData,
                            Long submittedBy, String submittedByUsername) {
        long queuedCount = jobRepository.countByStatus(AsyncJobStatus.QUEUED);
        if (queuedCount >= maxQueued) {
            throw new RuntimeException("Job queue is full (" + maxQueued + " queued). Try again later.");
        }

        AsyncJob job = new AsyncJob();
        job.setJobType(type);
        job.setDescription(description);
        job.setInputData(inputData);
        job.setSubmittedBy(submittedBy);
        job.setSubmittedByUsername(submittedByUsername);
        job.setStatus(AsyncJobStatus.QUEUED);
        return jobRepository.save(job);
    }

    @Transactional
    public AsyncJob markRunning(Long jobId) {
        AsyncJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        job.setStatus(AsyncJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }

    @Transactional
    public AsyncJob markCompleted(Long jobId, String resultData) {
        AsyncJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        job.setStatus(AsyncJobStatus.COMPLETED);
        job.setResultData(resultData);
        job.setProgressPercent(100);
        job.setCompletedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }

    @Transactional
    public AsyncJob markFailed(Long jobId, String errorMessage) {
        AsyncJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        job.setStatus(AsyncJobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setCompletedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }

    @Transactional
    public void updateProgress(Long jobId, int percent) {
        AsyncJob job = jobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setProgressPercent(Math.min(percent, 100));
            jobRepository.save(job);
        }
    }

    @Async("jobQueueExecutor")
    public void executeAsync(Long jobId, Consumer<AsyncJob> task) {
        AsyncJob job = markRunning(jobId);
        try {
            task.accept(job);
            if (job.getStatus() != AsyncJobStatus.COMPLETED) {
                markCompleted(jobId, job.getResultData());
            }
        } catch (Exception e) {
            log.error("Async job {} failed: {}", jobId, e.getMessage(), e);
            markFailed(jobId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AsyncJob> findAll() {
        return jobRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<AsyncJob> findById(Long id) {
        return jobRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new LinkedHashMap<>();
        long queued = jobRepository.countByStatus(AsyncJobStatus.QUEUED);
        long running = jobRepository.countByStatus(AsyncJobStatus.RUNNING);
        long failed = jobRepository.countByStatus(AsyncJobStatus.FAILED);

        health.put("queued", queued);
        health.put("running", running);
        health.put("failed", failed);
        health.put("maxQueued", maxQueued);
        health.put("maxRunning", maxRunning);
        health.put("warnThreshold", warnThreshold);

        String status;
        if (queued >= maxQueued) {
            status = "CRITICAL";
        } else if (queued >= warnThreshold) {
            status = "WARNING";
        } else {
            status = "HEALTHY";
        }
        health.put("status", status);

        for (AsyncJobType type : AsyncJobType.values()) {
            long typeQueued = jobRepository.countByJobTypeAndStatus(type, AsyncJobStatus.QUEUED);
            health.put("queued_" + type.name().toLowerCase(), typeQueued);
        }

        return health;
    }
}
