package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.ExportStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "export_jobs", indexes = {
    @Index(name = "idx_export_status", columnList = "status"),
    @Index(name = "idx_export_user", columnList = "created_by")
})
public class ExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "export_type", nullable = false, length = 50)
    private String exportType;

    @Column(name = "file_format", nullable = false, length = 10)
    private String fileFormat;

    @Column(name = "search_criteria_json", columnDefinition = "TEXT")
    private String searchCriteriaJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExportStatus status = ExportStatus.QUEUED;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExportType() { return exportType; }
    public void setExportType(String exportType) { this.exportType = exportType; }
    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public String getSearchCriteriaJson() { return searchCriteriaJson; }
    public void setSearchCriteriaJson(String searchCriteriaJson) { this.searchCriteriaJson = searchCriteriaJson; }
    public ExportStatus getStatus() { return status; }
    public void setStatus(ExportStatus status) { this.status = status; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
