package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.DimensionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DimensionDefinitionRepository extends JpaRepository<DimensionDefinition, Long> {

    Optional<DimensionDefinition> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<DimensionDefinition> findByActiveTrue();
}
