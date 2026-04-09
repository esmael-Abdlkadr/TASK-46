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
class RecruitingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessCandidateList() throws Exception {
        mockMvc.perform(get("/recruiter/candidates"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessJobProfiles() throws Exception {
        mockMvc.perform(get("/recruiter/job-profiles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessSearch() throws Exception {
        mockMvc.perform(get("/recruiter/search"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessPipeline() throws Exception {
        mockMvc.perform(get("/recruiter/pipeline"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessTalentPools() throws Exception {
        mockMvc.perform(get("/recruiter/talent-pools"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeClerkCannotAccessRecruitingFeatures() throws Exception {
        mockMvc.perform(get("/recruiter/candidates"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/recruiter/search"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/recruiter/pipeline"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessRecruitingFeatures() throws Exception {
        mockMvc.perform(get("/recruiter/candidates"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/recruiter/search"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedCannotAccessRecruiting() throws Exception {
        mockMvc.perform(get("/recruiter/candidates"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessNewCandidateForm() throws Exception {
        mockMvc.perform(get("/recruiter/candidates/new"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessNewJobProfileForm() throws Exception {
        mockMvc.perform(get("/recruiter/job-profiles/new"))
                .andExpect(status().isOk());
    }
}
