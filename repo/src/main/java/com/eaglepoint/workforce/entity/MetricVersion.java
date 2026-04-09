package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.MetricVersionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "metric_versions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_metric_version", columnNames = {"metric_id", "version_number"})
}, indexes = {
    @Index(name = "idx_mv_metric", columnList = "metric_id"),
    @Index(name = "idx_mv_status", columnList = "status")
})
public class MetricVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private MetricDefinition metricDefinition;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetricVersionStatus status = MetricVersionStatus.DRAFT;

    @Column(name = "definition_snapshot", nullable = false, columnDefinition = "TEXT")
    private String definitionSnapshot;

    @Column(name = "change_description", length = 1000)
    private String changeDescription;

    @Column(name = "published_by", length = 50)
    private String publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MetricDefinition getMetricDefinition() { return metricDefinition; }
    public void setMetricDefinition(MetricDefinition metricDefinition) { this.metricDefinition = metricDefinition; }
    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
    public MetricVersionStatus getStatus() { return status; }
    public void setStatus(MetricVersionStatus status) { this.status = status; }
    public String getDefinitionSnapshot() { return definitionSnapshot; }
    public void setDefinitionSnapshot(String definitionSnapshot) { this.definitionSnapshot = definitionSnapshot; }
    public String getChangeDescription() { return changeDescription; }
    public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
