package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.MetricDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MetricDefinitionRepository extends JpaRepository<MetricDefinition, Long> {

    Optional<MetricDefinition> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<MetricDefinition> findByActiveTrue();

    List<MetricDefinition> findByDerivedTrue();

    List<MetricDefinition> findByDerivedFalse();

    @Query("SELECT m FROM MetricDefinition m LEFT JOIN FETCH m.versions WHERE m.id = :id")
    Optional<MetricDefinition> findByIdWithVersions(@Param("id") Long id);

    @Query("SELECT m FROM MetricDefinition m LEFT JOIN FETCH m.dimensionLinks dl " +
           "LEFT JOIN FETCH dl.dimension WHERE m.id = :id")
    Optional<MetricDefinition> findByIdWithDimensions(@Param("id") Long id);

    @Query("SELECT m FROM MetricDefinition m LEFT JOIN FETCH m.windowCalculations WHERE m.id = :id")
    Optional<MetricDefinition> findByIdWithWindows(@Param("id") Long id);
}
