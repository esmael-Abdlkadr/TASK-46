package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.*;
import com.eaglepoint.workforce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MetricDefinitionServiceTest {

    @Autowired
    private MetricDefinitionService metricService;

    @Autowired
    private MetricDefinitionRepository metricRepo;

    private MetricDefinition baseMetricA;
    private MetricDefinition baseMetricB;
    private MetricDefinition derivedMetric;

    @BeforeEach
    void setUp() {
        baseMetricA = new MetricDefinition();
        baseMetricA.setSlug("total-hires");
        baseMetricA.setName("Total Hires");
        baseMetricA.setDataType(MetricDataType.COUNT);
        baseMetricA.setAggregationType(AggregationType.COUNT);
        baseMetricA.setSourceTable("candidate_profiles");
        baseMetricA.setSourceColumn("id");
        baseMetricA.setFilterExpression("pipeline_stage = 'HIRED'");
        baseMetricA = metricRepo.save(baseMetricA);

        baseMetricB = new MetricDefinition();
        baseMetricB.setSlug("total-applications");
        baseMetricB.setName("Total Applications");
        baseMetricB.setDataType(MetricDataType.COUNT);
        baseMetricB.setAggregationType(AggregationType.COUNT);
        baseMetricB.setSourceTable("candidate_profiles");
        baseMetricB.setSourceColumn("id");
        baseMetricB = metricRepo.save(baseMetricB);

        derivedMetric = new MetricDefinition();
        derivedMetric.setSlug("hire-rate");
        derivedMetric.setName("Hire Rate");
        derivedMetric.setDataType(MetricDataType.PERCENTAGE);
        derivedMetric.setAggregationType(AggregationType.AVG);
        derivedMetric.setDerived(true);
        derivedMetric.setFormula("total-hires / total-applications * 100");
        derivedMetric.setUnit("%");
        derivedMetric = metricRepo.save(derivedMetric);
    }

    @Test
    void createMetricDefinition() {
        assertNotNull(baseMetricA.getId());
        assertEquals("total-hires", baseMetricA.getSlug());
    }

    @Test
    void findBaseAndDerivedSeparately() {
        List<MetricDefinition> base = metricService.findBaseMetrics();
        List<MetricDefinition> derived = metricService.findDerivedMetrics();

        assertTrue(base.stream().anyMatch(m -> m.getSlug().equals("total-hires")));
        assertTrue(derived.stream().anyMatch(m -> m.getSlug().equals("hire-rate")));
    }

    // === Version Management ===

    @Test
    void createDraftVersion() {
        MetricVersion draft = metricService.createDraftVersion(baseMetricA.getId(), "Initial version");

        assertNotNull(draft.getId());
        assertEquals(1, draft.getVersionNumber());
        assertEquals(MetricVersionStatus.DRAFT, draft.getStatus());
        assertTrue(draft.getDefinitionSnapshot().contains("total-hires"));
    }

    @Test
    void publishVersionRecordsPublisher() {
        MetricVersion draft = metricService.createDraftVersion(baseMetricA.getId(), "v1");
        MetricVersion published = metricService.publishVersion(draft.getId(), "admin");

        assertEquals(MetricVersionStatus.PUBLISHED, published.getStatus());
        assertEquals("admin", published.getPublishedBy());
        assertNotNull(published.getPublishedAt());
    }

    @Test
    void publishDeprecatesPreviousVersion() {
        MetricVersion v1 = metricService.createDraftVersion(baseMetricA.getId(), "v1");
        metricService.publishVersion(v1.getId(), "admin");

        MetricVersion v2 = metricService.createDraftVersion(baseMetricA.getId(), "v2");
        metricService.publishVersion(v2.getId(), "admin");

        List<MetricVersion> history = metricService.getVersionHistory(baseMetricA.getId());
        MetricVersion v1Reloaded = history.stream()
                .filter(v -> v.getVersionNumber() == 1).findFirst().orElseThrow();
        assertEquals(MetricVersionStatus.DEPRECATED, v1Reloaded.getStatus());
    }

    @Test
    void rollbackRestoresDefinition() {
        // Publish v1
        MetricVersion v1 = metricService.createDraftVersion(baseMetricA.getId(), "v1 original");
        metricService.publishVersion(v1.getId(), "admin");

        // Change the metric
        baseMetricA.setName("Total Hires - Modified");
        metricRepo.save(baseMetricA);

        // Create and publish v2
        MetricVersion v2 = metricService.createDraftVersion(baseMetricA.getId(), "v2 modified");
        metricService.publishVersion(v2.getId(), "admin");

        // Rollback to v1
        MetricVersion rollback = metricService.rollbackToVersion(baseMetricA.getId(), 1, "admin");

        assertEquals(MetricVersionStatus.PUBLISHED, rollback.getStatus());
        assertTrue(rollback.getChangeDescription().contains("Rollback to version 1"));

        // The definition snapshot should match v1's snapshot
        assertEquals(v1.getDefinitionSnapshot(), rollback.getDefinitionSnapshot());
    }

    @Test
    void onlyDraftCanBePublished() {
        MetricVersion v1 = metricService.createDraftVersion(baseMetricA.getId(), "v1");
        metricService.publishVersion(v1.getId(), "admin");

        assertThrows(RuntimeException.class, () ->
                metricService.publishVersion(v1.getId(), "admin"));
    }

    @Test
    void versionNumbersAutoIncrement() {
        metricService.createDraftVersion(baseMetricA.getId(), "v1");
        metricService.createDraftVersion(baseMetricA.getId(), "v2");
        MetricVersion v3 = metricService.createDraftVersion(baseMetricA.getId(), "v3");

        assertEquals(3, v3.getVersionNumber());
    }

    // === Lineage ===

    @Test
    void addLineageTracksDependency() {
        metricService.addLineage(derivedMetric.getId(), baseMetricA.getId(),
                "NUMERATOR", "Counts hires for rate calculation");

        List<MetricLineage> sources = metricService.getSourceMetrics(derivedMetric.getId());
        assertEquals(1, sources.size());
        assertEquals(baseMetricA.getId(), sources.get(0).getSourceMetric().getId());
    }

    @Test
    void getDependentsShowsDownstream() {
        metricService.addLineage(derivedMetric.getId(), baseMetricA.getId(),
                "NUMERATOR", "Hires count");

        List<MetricLineage> dependents = metricService.getDependentMetrics(baseMetricA.getId());
        assertEquals(1, dependents.size());
        assertEquals(derivedMetric.getId(), dependents.get(0).getDerivedMetric().getId());
    }

    @Test
    void selfDependencyRejected() {
        assertThrows(RuntimeException.class, () ->
                metricService.addLineage(baseMetricA.getId(), baseMetricA.getId(), "INPUT", "self"));
    }

    @Test
    void circularDependencyRejected() {
        metricService.addLineage(derivedMetric.getId(), baseMetricA.getId(), "INPUT", "a->d");

        assertThrows(RuntimeException.class, () ->
                metricService.addLineage(baseMetricA.getId(), derivedMetric.getId(), "INPUT", "d->a circular"));
    }

    @Test
    void fullDownstreamImpactTraversesTransitively() {
        // A -> D (derived depends on A)
        metricService.addLineage(derivedMetric.getId(), baseMetricA.getId(), "NUMERATOR", "");

        // Create another derived metric that depends on derivedMetric
        MetricDefinition level2 = new MetricDefinition();
        level2.setSlug("hire-rate-smoothed");
        level2.setName("Hire Rate Smoothed");
        level2.setDataType(MetricDataType.PERCENTAGE);
        level2.setAggregationType(AggregationType.AVG);
        level2.setDerived(true);
        level2.setFormula("rolling_avg(hire-rate, 30)");
        level2 = metricRepo.save(level2);

        metricService.addLineage(level2.getId(), derivedMetric.getId(), "INPUT", "");

        // Impact of changing A should include both derived and level2
        List<MetricDefinition> impact = metricService.getFullDownstreamImpact(baseMetricA.getId());
        assertEquals(2, impact.size());
        assertTrue(impact.stream().anyMatch(m -> m.getSlug().equals("hire-rate")));
        assertTrue(impact.stream().anyMatch(m -> m.getSlug().equals("hire-rate-smoothed")));
    }

    // === Dimensions ===

    @Test
    void createDimension() {
        DimensionDefinition dim = new DimensionDefinition();
        dim.setSlug("department");
        dim.setName("Department");
        dim.setSourceTable("departments");
        dim.setSourceColumn("name");
        dim = metricService.saveDimension(dim);

        assertNotNull(dim.getId());
    }

    @Test
    void linkDimensionToMetric() {
        DimensionDefinition dim = new DimensionDefinition();
        dim.setSlug("location-dim");
        dim.setName("Location");
        dim.setSourceTable("sites");
        dim.setSourceColumn("zone");
        dim = metricService.saveDimension(dim);

        metricService.linkDimension(baseMetricA.getId(), dim.getId(), "candidate_profiles.location = sites.zone");

        MetricDefinition reloaded = metricRepo.findByIdWithDimensions(baseMetricA.getId()).orElseThrow();
        assertEquals(1, reloaded.getDimensionLinks().size());
    }
}
