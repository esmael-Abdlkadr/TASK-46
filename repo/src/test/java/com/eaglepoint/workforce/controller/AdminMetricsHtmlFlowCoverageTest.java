package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.entity.DimensionDefinition;
import com.eaglepoint.workforce.entity.MetricDefinition;
import com.eaglepoint.workforce.entity.MetricLineage;
import com.eaglepoint.workforce.entity.MetricVersion;
import com.eaglepoint.workforce.enums.AggregationType;
import com.eaglepoint.workforce.enums.MetricDataType;
import com.eaglepoint.workforce.repository.DimensionDefinitionRepository;
import com.eaglepoint.workforce.repository.MetricDefinitionRepository;
import com.eaglepoint.workforce.repository.MetricLineageRepository;
import com.eaglepoint.workforce.service.MetricDefinitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminMetricsHtmlFlowCoverageTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MetricDefinitionService metricService;
    @Autowired
    private MetricDefinitionRepository metricRepo;
    @Autowired
    private DimensionDefinitionRepository dimensionRepo;
    @Autowired
    private MetricLineageRepository lineageRepo;

    private MetricDefinition sourceMetric;
    private MetricDefinition derivedMetric;

    @BeforeEach
    void setup() {
        String suf = Long.toHexString(System.nanoTime());
        sourceMetric = new MetricDefinition();
        sourceMetric.setSlug("src-" + suf);
        sourceMetric.setName("Source " + suf);
        sourceMetric.setDataType(MetricDataType.COUNT);
        sourceMetric.setAggregationType(AggregationType.COUNT);
        sourceMetric.setSourceTable("t");
        sourceMetric.setSourceColumn("c");
        sourceMetric = metricRepo.save(sourceMetric);

        derivedMetric = new MetricDefinition();
        derivedMetric.setSlug("drv-" + suf);
        derivedMetric.setName("Derived " + suf);
        derivedMetric.setDataType(MetricDataType.COUNT);
        derivedMetric.setAggregationType(AggregationType.COUNT);
        derivedMetric.setDerived(true);
        derivedMetric.setFormula(sourceMetric.getSlug() + " * 2");
        derivedMetric = metricRepo.save(derivedMetric);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void publish_versionDetail_rollback_lineage_dimensionLink() throws Exception {
        MetricVersion draft = metricService.createDraftVersion(sourceMetric.getId(), "html-flow");
        mockMvc.perform(post("/admin/metrics/versions/" + draft.getId() + "/publish").with(csrf()))
                .andExpect(status().is3xxRedirection());

        MetricVersion published = metricService.findVersionById(draft.getId()).orElseThrow();

        mockMvc.perform(get("/admin/metrics/versions/" + published.getId()))
                .andExpect(status().isOk());

        MetricVersion draft2 = metricService.createDraftVersion(sourceMetric.getId(), "v2");
        metricService.publishVersion(draft2.getId(), "admin");

        mockMvc.perform(post("/admin/metrics/" + sourceMetric.getId() + "/versions/rollback").with(csrf())
                        .param("targetVersion", "1"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/admin/metrics/" + derivedMetric.getId() + "/lineage/add").with(csrf())
                        .param("sourceMetricId", sourceMetric.getId().toString())
                        .param("relationshipType", "INPUT")
                        .param("contributionDescription", "coverage"))
                .andExpect(status().is3xxRedirection());

        MetricLineage link = lineageRepo.findSourcesForMetric(derivedMetric.getId()).stream()
                .reduce((a, b) -> b)
                .orElseThrow();

        mockMvc.perform(post("/admin/metrics/lineage/" + link.getId() + "/remove").with(csrf())
                        .param("metricId", derivedMetric.getId().toString()))
                .andExpect(status().is3xxRedirection());

        String dimSlug = "dim-" + Long.toHexString(System.nanoTime());
        mockMvc.perform(post("/admin/metrics/dimensions/save").with(csrf())
                        .param("slug", dimSlug)
                        .param("name", "Dim Coverage"))
                .andExpect(status().is3xxRedirection());

        DimensionDefinition dim = dimensionRepo.findBySlug(dimSlug).orElseThrow();

        mockMvc.perform(post("/admin/metrics/" + sourceMetric.getId() + "/dimensions/link").with(csrf())
                        .param("dimensionId", dim.getId().toString())
                        .param("joinExpression", "d.id = m.dept_id"))
                .andExpect(status().is3xxRedirection());
    }
}
