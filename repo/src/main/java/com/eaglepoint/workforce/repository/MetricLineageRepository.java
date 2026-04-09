package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.MetricLineage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MetricLineageRepository extends JpaRepository<MetricLineage, Long> {

    @Query("SELECT ml FROM MetricLineage ml JOIN FETCH ml.sourceMetric WHERE ml.derivedMetric.id = :metricId")
    List<MetricLineage> findSourcesForMetric(@Param("metricId") Long metricId);

    @Query("SELECT ml FROM MetricLineage ml JOIN FETCH ml.derivedMetric WHERE ml.sourceMetric.id = :metricId")
    List<MetricLineage> findDependentsOfMetric(@Param("metricId") Long metricId);

    void deleteByDerivedMetricId(Long derivedMetricId);
}
