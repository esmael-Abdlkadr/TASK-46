package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.MetricVersion;
import com.eaglepoint.workforce.enums.MetricVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MetricVersionRepository extends JpaRepository<MetricVersion, Long> {

    List<MetricVersion> findByMetricDefinitionIdOrderByVersionNumberDesc(Long metricId);

    Optional<MetricVersion> findByMetricDefinitionIdAndVersionNumber(Long metricId, Integer versionNumber);

    @Query("SELECT mv FROM MetricVersion mv WHERE mv.metricDefinition.id = :metricId " +
           "AND mv.status = 'PUBLISHED' ORDER BY mv.versionNumber DESC")
    List<MetricVersion> findPublishedByMetricId(@Param("metricId") Long metricId);

    @Query("SELECT COALESCE(MAX(mv.versionNumber), 0) FROM MetricVersion mv " +
           "WHERE mv.metricDefinition.id = :metricId")
    Integer findMaxVersionNumber(@Param("metricId") Long metricId);

    List<MetricVersion> findByStatus(MetricVersionStatus status);
}
