package com.eaglepoint.workforce.security;

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
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedUserRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginPageIsAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessRecruiterDashboard() throws Exception {
        mockMvc.perform(get("/recruiter/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void dispatchCanAccessDispatchDashboard() throws Exception {
        mockMvc.perform(get("/dispatch/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCanAccessFinanceDashboard() throws Exception {
        mockMvc.perform(get("/finance/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessAuditLog() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCannotAccessAuditLog() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isForbidden());
    }
}
