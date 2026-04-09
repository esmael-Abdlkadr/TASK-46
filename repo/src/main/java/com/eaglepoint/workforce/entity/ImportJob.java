package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.ImportStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs", indexes = {
    @Index(name = "idx_import_status", columnList = "status"),
    @Index(name = "idx_import_fingerprint", columnList = "file_fingerprint")
})
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_file_name", nullable = false, length = 200)
    private String originalFileName;

    @Column(name = "import_type", nullable = false, length = 50)
    private String importType;

    @Column(name = "file_fingerprint", nullable = false, length = 64)
    private String fileFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ImportStatus status = ImportStatus.QUEUED;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "success_rows")
    private Integer successRows;

    @Column(name = "error_rows")
    private Integer errorRows;

    @Column(name = "error_report", columnDefinition = "TEXT")
    private String errorReport;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }
    public String getFileFingerprint() { return fileFingerprint; }
    public void setFileFingerprint(String fileFingerprint) { this.fileFingerprint = fileFingerprint; }
    public ImportStatus getStatus() { return status; }
    public void setStatus(ImportStatus status) { this.status = status; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getSuccessRows() { return successRows; }
    public void setSuccessRows(Integer successRows) { this.successRows = successRows; }
    public Integer getErrorRows() { return errorRows; }
    public void setErrorRows(Integer errorRows) { this.errorRows = errorRows; }
    public String getErrorReport() { return errorReport; }
    public void setErrorReport(String errorReport) { this.errorReport = errorReport; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
