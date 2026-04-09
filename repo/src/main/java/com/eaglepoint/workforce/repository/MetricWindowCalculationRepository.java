package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.MetricWindowCalculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricWindowCalculationRepository extends JpaRepository<MetricWindowCalculation, Long> {
    List<MetricWindowCalculation> findByMetricDefinitionId(Long metricId);
}
