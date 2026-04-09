package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.AggregationType;
import com.eaglepoint.workforce.enums.WindowType;
import jakarta.persistence.*;

@Entity
@Table(name = "metric_window_calculations")
public class MetricWindowCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private MetricDefinition metricDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "window_type", nullable = false, length = 30)
    private WindowType windowType;

    @Enumerated(EnumType.STRING)
    @Column(name = "window_aggregation", nullable = false, length = 20)
    private AggregationType windowAggregation;

    @Column(name = "custom_window_days")
    private Integer customWindowDays;

    @Column(length = 200)
    private String label;

    public MetricWindowCalculation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MetricDefinition getMetricDefinition() { return metricDefinition; }
    public void setMetricDefinition(MetricDefinition metricDefinition) { this.metricDefinition = metricDefinition; }
    public WindowType getWindowType() { return windowType; }
    public void setWindowType(WindowType windowType) { this.windowType = windowType; }
    public AggregationType getWindowAggregation() { return windowAggregation; }
    public void setWindowAggregation(AggregationType windowAggregation) { this.windowAggregation = windowAggregation; }
    public Integer getCustomWindowDays() { return customWindowDays; }
    public void setCustomWindowDays(Integer customWindowDays) { this.customWindowDays = customWindowDays; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
