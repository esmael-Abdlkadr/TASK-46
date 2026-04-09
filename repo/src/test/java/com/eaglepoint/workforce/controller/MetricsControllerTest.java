package com.eaglepoint.workforce.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessMetricsList() throws Exception {
        mockMvc.perform(get("/admin/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessNewMetricForm() throws Exception {
        mockMvc.perform(get("/admin/metrics/new"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessDimensions() throws Exception {
        mockMvc.perform(get("/admin/metrics/dimensions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCannotAccessMetrics() throws Exception {
        mockMvc.perform(get("/admin/metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void dispatchCannotAccessMetrics() throws Exception {
        mockMvc.perform(get("/admin/metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotAccessMetrics() throws Exception {
        mockMvc.perform(get("/admin/metrics"))
                .andExpect(status().is3xxRedirection());
    }
}
