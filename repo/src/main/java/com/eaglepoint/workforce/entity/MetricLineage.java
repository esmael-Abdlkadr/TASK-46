package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "metric_lineage", uniqueConstraints = {
    @UniqueConstraint(name = "uk_lineage_pair", columnNames = {"derived_metric_id", "source_metric_id"})
}, indexes = {
    @Index(name = "idx_lineage_derived", columnList = "derived_metric_id"),
    @Index(name = "idx_lineage_source", columnList = "source_metric_id")
})
public class MetricLineage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "derived_metric_id", nullable = false)
    private MetricDefinition derivedMetric;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_metric_id", nullable = false)
    private MetricDefinition sourceMetric;

    @Column(name = "relationship_type", nullable = false, length = 30)
    private String relationshipType;

    @Column(name = "contribution_description", length = 500)
    private String contributionDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public MetricLineage() {}

    public MetricLineage(MetricDefinition derivedMetric, MetricDefinition sourceMetric,
                          String relationshipType, String contributionDescription) {
        this.derivedMetric = derivedMetric;
        this.sourceMetric = sourceMetric;
        this.relationshipType = relationshipType;
        this.contributionDescription = contributionDescription;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MetricDefinition getDerivedMetric() { return derivedMetric; }
    public void setDerivedMetric(MetricDefinition derivedMetric) { this.derivedMetric = derivedMetric; }
    public MetricDefinition getSourceMetric() { return sourceMetric; }
    public void setSourceMetric(MetricDefinition sourceMetric) { this.sourceMetric = sourceMetric; }
    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }
    public String getContributionDescription() { return contributionDescription; }
    public void setContributionDescription(String contributionDescription) { this.contributionDescription = contributionDescription; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
