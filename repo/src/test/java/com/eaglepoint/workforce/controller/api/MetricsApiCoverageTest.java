package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.entity.MetricDefinition;
import com.eaglepoint.workforce.entity.MetricVersion;
import com.eaglepoint.workforce.enums.AggregationType;
import com.eaglepoint.workforce.enums.MetricDataType;
import com.eaglepoint.workforce.enums.MetricVersionStatus;
import com.eaglepoint.workforce.repository.MetricDefinitionRepository;
import com.eaglepoint.workforce.repository.MetricVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers GET /api/v1/metrics/{id}/versions, POST /api/v1/metrics/versions/{id}/publish,
 * and POST /api/v1/metrics/{id}/rollback. Asserts RBAC (admin-only), happy paths, and error envelopes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MetricsApiCoverageTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MetricDefinitionRepository metricRepo;
    @Autowired private MetricVersionRepository versionRepo;

    private MetricDefinition metric;
    private MetricVersion publishedVersion;

    @BeforeEach
    void setup() {
        MetricDefinition m = new MetricDefinition();
        m.setSlug("api-cov-metric-" + System.nanoTime());
        m.setName("API Coverage Metric");
        m.setDataType(MetricDataType.COUNT);
        m.setAggregationType(AggregationType.COUNT);
        m.setSourceTable("users");
        m.setSourceColumn("id");
        m.setActive(true);
        m.setDerived(false);
        metric = metricRepo.save(m);

        String snapshot = "{\"slug\":\"" + m.getSlug() + "\",\"name\":\"API Coverage Metric\"," +
                "\"dataType\":\"COUNT\",\"aggregationType\":\"COUNT\"," +
                "\"derived\":false,\"active\":true}";

        MetricVersion v = new MetricVersion();
        v.setMetricDefinition(metric);
        v.setVersionNumber(1);
        v.setStatus(MetricVersionStatus.PUBLISHED);
        v.setDefinitionSnapshot(snapshot);
        v.setChangeDescription("Initial version");
        v.setPublishedBy("admin");
        v.setPublishedAt(LocalDateTime.now());
        publishedVersion = versionRepo.save(v);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void getVersions_happyPath_returnsVersionList() throws Exception {
        mockMvc.perform(get("/api/v1/metrics/" + metric.getId() + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andExpect(jsonPath("$[0].status").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void getVersions_noVersionsForMetric_returnsEmptyList() throws Exception {
        MetricDefinition empty = new MetricDefinition();
        empty.setSlug("api-cov-empty-" + System.nanoTime());
        empty.setName("Empty Metric");
        empty.setDataType(MetricDataType.COUNT);
        empty.setAggregationType(AggregationType.COUNT);
        empty.setActive(true);
        empty.setDerived(false);
        empty = metricRepo.save(empty);

        mockMvc.perform(get("/api/v1/metrics/" + empty.getId() + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void rollback_happyPath_returnsNewPublishedVersion() throws Exception {
        String body = "{\"targetVersion\":1}";
        mockMvc.perform(post("/api/v1/metrics/" + metric.getId() + "/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionNumber").isNumber())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void publish_draftVersion_returnsPublished() throws Exception {
        String snapshot = publishedVersion.getDefinitionSnapshot();
        MetricVersion draft = new MetricVersion();
        draft.setMetricDefinition(metric);
        draft.setVersionNumber(2);
        draft.setStatus(MetricVersionStatus.DRAFT);
        draft.setDefinitionSnapshot(snapshot);
        draft.setChangeDescription("publish API test draft");
        draft = versionRepo.save(draft);

        mockMvc.perform(post("/api/v1/metrics/versions/" + draft.getId() + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedBy").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void rollback_nonExistentMetric_returnsInternalErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/metrics/99999/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetVersion\":1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/v1/metrics/99999/rollback"));
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void getVersions_nonAdmin_denied() throws Exception {
        mockMvc.perform(get("/api/v1/metrics/" + metric.getId() + "/versions"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 403 || result.getResponse()
                                    .getContentAsString().toLowerCase().contains("denied"),
                            "Non-admin must be denied versions endpoint, got status=" + status);
                });
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void rollback_nonAdmin_denied() throws Exception {
        mockMvc.perform(post("/api/v1/metrics/" + metric.getId() + "/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetVersion\":1}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 403 || result.getResponse()
                                    .getContentAsString().toLowerCase().contains("denied"),
                            "Finance clerk must be denied rollback endpoint, got status=" + status);
                });
    }

    @Test
    void unauthenticated_getVersions_redirects() throws Exception {
        mockMvc.perform(get("/api/v1/metrics/" + metric.getId() + "/versions"))
                .andExpect(status().is3xxRedirection());
    }
}
