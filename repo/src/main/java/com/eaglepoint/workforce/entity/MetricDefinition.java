package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.AggregationType;
import com.eaglepoint.workforce.enums.MetricDataType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "metric_definitions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_metric_slug", columnNames = "slug")
})
public class MetricDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private MetricDataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", nullable = false, length = 20)
    private AggregationType aggregationType;

    @Column(name = "source_table", length = 100)
    private String sourceTable;

    @Column(name = "source_column", length = 100)
    private String sourceColumn;

    @Column(name = "filter_expression", length = 500)
    private String filterExpression;

    @Column(length = 20)
    private String unit;

    @Column(nullable = false)
    private boolean derived = false;

    @Column(name = "formula", length = 500)
    private String formula;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "owner_username", length = 50)
    private String ownerUsername;

    @OneToMany(mappedBy = "metricDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber DESC")
    private List<MetricVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "metricDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetricDimensionLink> dimensionLinks = new ArrayList<>();

    @OneToMany(mappedBy = "metricDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MetricWindowCalculation> windowCalculations = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MetricDataType getDataType() { return dataType; }
    public void setDataType(MetricDataType dataType) { this.dataType = dataType; }
    public AggregationType getAggregationType() { return aggregationType; }
    public void setAggregationType(AggregationType aggregationType) { this.aggregationType = aggregationType; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public String getSourceColumn() { return sourceColumn; }
    public void setSourceColumn(String sourceColumn) { this.sourceColumn = sourceColumn; }
    public String getFilterExpression() { return filterExpression; }
    public void setFilterExpression(String filterExpression) { this.filterExpression = filterExpression; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isDerived() { return derived; }
    public void setDerived(boolean derived) { this.derived = derived; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
    public List<MetricVersion> getVersions() { return versions; }
    public void setVersions(List<MetricVersion> versions) { this.versions = versions; }
    public List<MetricDimensionLink> getDimensionLinks() { return dimensionLinks; }
    public void setDimensionLinks(List<MetricDimensionLink> dimensionLinks) { this.dimensionLinks = dimensionLinks; }
    public List<MetricWindowCalculation> getWindowCalculations() { return windowCalculations; }
    public void setWindowCalculations(List<MetricWindowCalculation> windowCalculations) { this.windowCalculations = windowCalculations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
