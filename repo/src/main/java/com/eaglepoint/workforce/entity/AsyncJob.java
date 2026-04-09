package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.AsyncJobStatus;
import com.eaglepoint.workforce.enums.AsyncJobType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "async_jobs", indexes = {
    @Index(name = "idx_ajob_status", columnList = "status"),
    @Index(name = "idx_ajob_type", columnList = "job_type"),
    @Index(name = "idx_ajob_created", columnList = "created_at")
})
public class AsyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private AsyncJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AsyncJobStatus status = AsyncJobStatus.QUEUED;

    @Column(length = 200)
    private String description;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "progress_percent")
    private Integer progressPercent = 0;

    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;

    @Column(name = "submitted_by_username", length = 50)
    private String submittedByUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AsyncJobType getJobType() { return jobType; }
    public void setJobType(AsyncJobType jobType) { this.jobType = jobType; }
    public AsyncJobStatus getStatus() { return status; }
    public void setStatus(AsyncJobStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }
    public String getResultData() { return resultData; }
    public void setResultData(String resultData) { this.resultData = resultData; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public Long getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(Long submittedBy) { this.submittedBy = submittedBy; }
    public String getSubmittedByUsername() { return submittedByUsername; }
    public void setSubmittedByUsername(String submittedByUsername) { this.submittedByUsername = submittedByUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
