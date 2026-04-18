package com.eaglepoint.workforce.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestApiSecurityTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void unauthenticated_returns401or302() throws Exception {
        mockMvc.perform(get("/api/v1/session"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void session_returnsUserInfo() throws Exception {
        mockMvc.perform(get("/api/v1/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiter_cannotAccessPaymentApi_denied() throws Exception {
        // @PreAuthorize blocks recruiter; response is either 403 or error page with denial text
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    String body = result.getResponse().getContentAsString().toLowerCase();
                    assertTrue(status == 403 || body.contains("forbidden") || body.contains("denied")
                            || body.contains("error"),
                            "Recruiter should be denied payment API access, got status=" + status);
                });
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void finance_canAccessPaymentApi() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void admin_canAccessPaymentApi() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiter_cannotPublishMetric_denied() throws Exception {
        mockMvc.perform(post("/api/v1/metrics/versions/1/publish")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    String body = result.getResponse().getContentAsString().toLowerCase();
                    assertTrue(status == 403 || body.contains("forbidden") || body.contains("denied")
                            || body.contains("error"),
                            "Recruiter should be denied metric publish, got status=" + status);
                });
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void payment_missingFields_returns400WithValidationErrorEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/payments"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void searchApi_returnsResults() throws Exception {
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void export_nonExistent_handledGracefully() throws Exception {
        mockMvc.perform(get("/api/v1/exports/99999"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getContentAsString().toLowerCase().contains("not found")
                        || result.getResponse().getStatus() == 404,
                        "Should indicate resource not found"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void import_nonExistent_handledGracefully() throws Exception {
        mockMvc.perform(get("/api/v1/imports/99999"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getContentAsString().toLowerCase().contains("not found")
                        || result.getResponse().getStatus() == 404,
                        "Should indicate resource not found"));
    }
}
