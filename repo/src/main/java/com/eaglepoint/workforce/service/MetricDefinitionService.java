package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.MetricVersionStatus;
import com.eaglepoint.workforce.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MetricDefinitionService {

    private final MetricDefinitionRepository metricRepo;
    private final MetricVersionRepository versionRepo;
    private final MetricLineageRepository lineageRepo;
    private final DimensionDefinitionRepository dimensionRepo;
    private final MetricWindowCalculationRepository windowRepo;
    private final ObjectMapper objectMapper;

    public MetricDefinitionService(MetricDefinitionRepository metricRepo,
                                    MetricVersionRepository versionRepo,
                                    MetricLineageRepository lineageRepo,
                                    DimensionDefinitionRepository dimensionRepo,
                                    MetricWindowCalculationRepository windowRepo,
                                    ObjectMapper objectMapper) {
        this.metricRepo = metricRepo;
        this.versionRepo = versionRepo;
        this.lineageRepo = lineageRepo;
        this.dimensionRepo = dimensionRepo;
        this.windowRepo = windowRepo;
        this.objectMapper = objectMapper;
    }

    // === Metric CRUD ===

    @Transactional(readOnly = true)
    public List<MetricDefinition> findAll() {
        return metricRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<MetricDefinition> findActive() {
        return metricRepo.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<MetricDefinition> findBaseMetrics() {
        return metricRepo.findByDerivedFalse();
    }

    @Transactional(readOnly = true)
    public List<MetricDefinition> findDerivedMetrics() {
        return metricRepo.findByDerivedTrue();
    }

    @Transactional(readOnly = true)
    public Optional<MetricDefinition> findById(Long id) {
        return metricRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<MetricDefinition> findByIdWithVersions(Long id) {
        return metricRepo.findByIdWithVersions(id);
    }

    @Transactional
    public MetricDefinition save(MetricDefinition metric) {
        return metricRepo.save(metric);
    }

    // === Version Management ===

    @Transactional
    public MetricVersion createDraftVersion(Long metricId, String changeDescription) {
        MetricDefinition metric = metricRepo.findById(metricId)
                .orElseThrow(() -> new RuntimeException("Metric not found: " + metricId));

        Integer maxVersion = versionRepo.findMaxVersionNumber(metricId);
        int nextVersion = maxVersion + 1;

        MetricVersion version = new MetricVersion();
        version.setMetricDefinition(metric);
        version.setVersionNumber(nextVersion);
        version.setStatus(MetricVersionStatus.DRAFT);
        version.setChangeDescription(changeDescription);
        version.setDefinitionSnapshot(snapshotMetric(metric));

        return versionRepo.save(version);
    }

    @Transactional
    public MetricVersion publishVersion(Long versionId, String publishedBy) {
        MetricVersion version = versionRepo.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));

        if (version.getStatus() != MetricVersionStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT versions can be published");
        }

        // Deprecate previous published versions
        List<MetricVersion> published = versionRepo.findPublishedByMetricId(
                version.getMetricDefinition().getId());
        for (MetricVersion pv : published) {
            pv.setStatus(MetricVersionStatus.DEPRECATED);
            versionRepo.save(pv);
        }

        version.setStatus(MetricVersionStatus.PUBLISHED);
        version.setPublishedBy(publishedBy);
        version.setPublishedAt(LocalDateTime.now());
        version.setDefinitionSnapshot(snapshotMetric(version.getMetricDefinition()));

        return versionRepo.save(version);
    }

    @Transactional
    public MetricVersion rollbackToVersion(Long metricId, Integer targetVersionNumber, String performedBy) {
        MetricDefinition metric = metricRepo.findById(metricId)
                .orElseThrow(() -> new RuntimeException("Metric not found: " + metricId));

        MetricVersion target = versionRepo.findByMetricDefinitionIdAndVersionNumber(metricId, targetVersionNumber)
                .orElseThrow(() -> new RuntimeException("Version " + targetVersionNumber + " not found"));

        // Mark current published as rolled back
        List<MetricVersion> currentPublished = versionRepo.findPublishedByMetricId(metricId);
        for (MetricVersion pv : currentPublished) {
            pv.setStatus(MetricVersionStatus.ROLLED_BACK);
            versionRepo.save(pv);
        }

        // Restore metric state from the target snapshot
        restoreFromSnapshot(metric, target.getDefinitionSnapshot());
        metricRepo.save(metric);

        // Create a new version recording the rollback
        Integer maxVersion = versionRepo.findMaxVersionNumber(metricId);
        MetricVersion rollbackVersion = new MetricVersion();
        rollbackVersion.setMetricDefinition(metric);
        rollbackVersion.setVersionNumber(maxVersion + 1);
        rollbackVersion.setStatus(MetricVersionStatus.PUBLISHED);
        rollbackVersion.setPublishedBy(performedBy);
        rollbackVersion.setPublishedAt(LocalDateTime.now());
        rollbackVersion.setChangeDescription("Rollback to version " + targetVersionNumber);
        rollbackVersion.setDefinitionSnapshot(target.getDefinitionSnapshot());

        return versionRepo.save(rollbackVersion);
    }

    @Transactional(readOnly = true)
    public List<MetricVersion> getVersionHistory(Long metricId) {
        return versionRepo.findByMetricDefinitionIdOrderByVersionNumberDesc(metricId);
    }

    @Transactional(readOnly = true)
    public Optional<MetricVersion> findVersionById(Long versionId) {
        return versionRepo.findById(versionId);
    }

    @Transactional(readOnly = true)
    public Optional<MetricVersion> getCurrentPublishedVersion(Long metricId) {
        List<MetricVersion> published = versionRepo.findPublishedByMetricId(metricId);
        return published.isEmpty() ? Optional.empty() : Optional.of(published.get(0));
    }

    // === Lineage ===

    @Transactional
    public MetricLineage addLineage(Long derivedMetricId, Long sourceMetricId,
                                     String relationshipType, String contributionDescription) {
        MetricDefinition derived = metricRepo.findById(derivedMetricId)
                .orElseThrow(() -> new RuntimeException("Derived metric not found"));
        MetricDefinition source = metricRepo.findById(sourceMetricId)
                .orElseThrow(() -> new RuntimeException("Source metric not found"));

        if (derivedMetricId.equals(sourceMetricId)) {
            throw new RuntimeException("A metric cannot depend on itself");
        }

        // Check for circular dependency
        if (wouldCreateCycle(sourceMetricId, derivedMetricId)) {
            throw new RuntimeException("Adding this lineage would create a circular dependency");
        }

        MetricLineage lineage = new MetricLineage(derived, source, relationshipType, contributionDescription);
        return lineageRepo.save(lineage);
    }

    @Transactional
    public void removeLineage(Long lineageId) {
        lineageRepo.deleteById(lineageId);
    }

    @Transactional(readOnly = true)
    public List<MetricLineage> getSourceMetrics(Long metricId) {
        return lineageRepo.findSourcesForMetric(metricId);
    }

    @Transactional(readOnly = true)
    public List<MetricLineage> getDependentMetrics(Long metricId) {
        return lineageRepo.findDependentsOfMetric(metricId);
    }

    @Transactional(readOnly = true)
    public List<MetricDefinition> getFullDownstreamImpact(Long metricId) {
        Set<Long> visited = new LinkedHashSet<>();
        collectDownstream(metricId, visited);
        visited.remove(metricId);
        return metricRepo.findAllById(visited);
    }

    private void collectDownstream(Long metricId, Set<Long> visited) {
        if (visited.contains(metricId)) return;
        visited.add(metricId);
        for (MetricLineage ml : lineageRepo.findDependentsOfMetric(metricId)) {
            collectDownstream(ml.getDerivedMetric().getId(), visited);
        }
    }

    private boolean wouldCreateCycle(Long sourceId, Long derivedId) {
        Set<Long> visited = new HashSet<>();
        return hasCycleDFS(sourceId, derivedId, visited);
    }

    private boolean hasCycleDFS(Long currentId, Long targetId, Set<Long> visited) {
        if (currentId.equals(targetId)) return true;
        if (visited.contains(currentId)) return false;
        visited.add(currentId);
        for (MetricLineage ml : lineageRepo.findSourcesForMetric(currentId)) {
            if (hasCycleDFS(ml.getSourceMetric().getId(), targetId, visited)) return true;
        }
        return false;
    }

    // === Dimensions ===

    @Transactional(readOnly = true)
    public List<DimensionDefinition> findAllDimensions() {
        return dimensionRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<DimensionDefinition> findActiveDimensions() {
        return dimensionRepo.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<DimensionDefinition> findDimensionById(Long id) {
        return dimensionRepo.findById(id);
    }

    @Transactional
    public DimensionDefinition saveDimension(DimensionDefinition dimension) {
        return dimensionRepo.save(dimension);
    }

    // === Dimension Links ===

    @Transactional
    public void linkDimension(Long metricId, Long dimensionId, String joinExpression) {
        MetricDefinition metric = metricRepo.findById(metricId)
                .orElseThrow(() -> new RuntimeException("Metric not found"));
        DimensionDefinition dimension = dimensionRepo.findById(dimensionId)
                .orElseThrow(() -> new RuntimeException("Dimension not found"));

        MetricDimensionLink link = new MetricDimensionLink(metric, dimension);
        link.setJoinExpression(joinExpression);
        metric.getDimensionLinks().add(link);
        metricRepo.save(metric);
    }

    // === Window Calculations ===

    @Transactional(readOnly = true)
    public List<MetricWindowCalculation> getWindowCalculations(Long metricId) {
        return windowRepo.findByMetricDefinitionId(metricId);
    }

    // === Snapshot helpers ===

    private String snapshotMetric(MetricDefinition metric) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("slug", metric.getSlug());
        snapshot.put("name", metric.getName());
        snapshot.put("description", metric.getDescription());
        snapshot.put("dataType", metric.getDataType().name());
        snapshot.put("aggregationType", metric.getAggregationType().name());
        snapshot.put("sourceTable", metric.getSourceTable());
        snapshot.put("sourceColumn", metric.getSourceColumn());
        snapshot.put("filterExpression", metric.getFilterExpression());
        snapshot.put("unit", metric.getUnit());
        snapshot.put("derived", metric.isDerived());
        snapshot.put("formula", metric.getFormula());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void restoreFromSnapshot(MetricDefinition metric, String snapshotJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> snapshot = objectMapper.readValue(snapshotJson, Map.class);
            if (snapshot.containsKey("name")) metric.setName((String) snapshot.get("name"));
            if (snapshot.containsKey("description")) metric.setDescription((String) snapshot.get("description"));
            if (snapshot.containsKey("sourceTable")) metric.setSourceTable((String) snapshot.get("sourceTable"));
            if (snapshot.containsKey("sourceColumn")) metric.setSourceColumn((String) snapshot.get("sourceColumn"));
            if (snapshot.containsKey("filterExpression")) metric.setFilterExpression((String) snapshot.get("filterExpression"));
            if (snapshot.containsKey("unit")) metric.setUnit((String) snapshot.get("unit"));
            if (snapshot.containsKey("formula")) metric.setFormula((String) snapshot.get("formula"));
        } catch (JsonProcessingException ignored) {}
    }
}
