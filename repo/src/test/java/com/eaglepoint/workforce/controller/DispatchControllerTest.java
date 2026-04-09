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
class DispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void supervisorCanAccessCollectors() throws Exception {
        mockMvc.perform(get("/dispatch/collectors"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void supervisorCanAccessSites() throws Exception {
        mockMvc.perform(get("/dispatch/sites"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void supervisorCanAccessAssignments() throws Exception {
        mockMvc.perform(get("/dispatch/assignments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void supervisorCanAccessNewCollectorForm() throws Exception {
        mockMvc.perform(get("/dispatch/collectors/new"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void supervisorCanAccessNewSiteForm() throws Exception {
        mockMvc.perform(get("/dispatch/sites/new"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void supervisorCanAccessNewAssignmentForm() throws Exception {
        mockMvc.perform(get("/dispatch/assignments/new"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCannotAccessDispatch() throws Exception {
        mockMvc.perform(get("/dispatch/collectors"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/dispatch/sites"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/dispatch/assignments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCannotAccessDispatch() throws Exception {
        mockMvc.perform(get("/dispatch/assignments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessDispatch() throws Exception {
        mockMvc.perform(get("/dispatch/collectors"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dispatch/sites"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/dispatch/assignments"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedCannotAccessDispatch() throws Exception {
        mockMvc.perform(get("/dispatch/collectors"))
                .andExpect(status().is3xxRedirection());
    }
}
