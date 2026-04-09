package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "metric_dimension_links", uniqueConstraints = {
    @UniqueConstraint(name = "uk_metric_dim", columnNames = {"metric_id", "dimension_id"})
})
public class MetricDimensionLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private MetricDefinition metricDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dimension_id", nullable = false)
    private DimensionDefinition dimension;

    @Column(name = "join_expression", length = 200)
    private String joinExpression;

    public MetricDimensionLink() {}

    public MetricDimensionLink(MetricDefinition metricDefinition, DimensionDefinition dimension) {
        this.metricDefinition = metricDefinition;
        this.dimension = dimension;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MetricDefinition getMetricDefinition() { return metricDefinition; }
    public void setMetricDefinition(MetricDefinition metricDefinition) { this.metricDefinition = metricDefinition; }
    public DimensionDefinition getDimension() { return dimension; }
    public void setDimension(DimensionDefinition dimension) { this.dimension = dimension; }
    public String getJoinExpression() { return joinExpression; }
    public void setJoinExpression(String joinExpression) { this.joinExpression = joinExpression; }
}
