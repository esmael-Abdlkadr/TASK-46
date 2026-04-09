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
class SearchExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "RECRUITER")
    void authenticatedCanAccessSearch() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void authenticatedCanAccessExports() throws Exception {
        mockMvc.perform(get("/exports"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void authenticatedCanAccessImports() throws Exception {
        mockMvc.perform(get("/imports"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedCannotAccessSearch() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessMasterData() throws Exception {
        mockMvc.perform(get("/masterdata/departments"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/masterdata/classes"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/masterdata/courses"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/masterdata/semesters"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void nonAdminCanAccessMasterData() throws Exception {
        mockMvc.perform(get("/masterdata/departments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void dispatchCanAccessSearchAndExports() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/exports"))
                .andExpect(status().isOk());
    }
}
