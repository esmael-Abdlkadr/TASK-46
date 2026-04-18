package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.enums.PipelineStage;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers write (POST) endpoints in /recruiter/candidates: create, edit, bulk-tag.
 * Verifies RBAC isolation between recruiter, finance, dispatch, and admin roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecruitingWriteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CandidateProfileRepository candidateRepo;

    private CandidateProfile candidate;

    @BeforeEach
    void setup() {
        CandidateProfile c = new CandidateProfile();
        c.setFirstName("Jane");
        c.setLastName("Testcandidate");
        c.setEmail("jane.tc@test.com");
        c.setPipelineStage(PipelineStage.SOURCED);
        candidate = candidateRepo.save(c);
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanCreateCandidate() throws Exception {
        mockMvc.perform(post("/recruiter/candidates/new").with(csrf())
                        .param("firstName", "John")
                        .param("lastName", "NewCandidate")
                        .param("email", "john.new@test.com")
                        .param("pipelineStage", "SOURCED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/candidates"));
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanAccessEditCandidateForm() throws Exception {
        mockMvc.perform(get("/recruiter/candidates/" + candidate.getId() + "/edit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanUpdateCandidate() throws Exception {
        mockMvc.perform(post("/recruiter/candidates/" + candidate.getId() + "/edit").with(csrf())
                        .param("firstName", "Jane")
                        .param("lastName", "UpdatedSurname")
                        .param("email", "jane.updated@test.com")
                        .param("pipelineStage", "SCREENING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/candidates/" + candidate.getId()));
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanBulkTagCandidates() throws Exception {
        mockMvc.perform(post("/recruiter/candidates/bulk-tag").with(csrf())
                        .param("candidateIds", candidate.getId().toString())
                        .param("tag", "priority"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/candidates"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeClerk_cannotCreateCandidate() throws Exception {
        mockMvc.perform(post("/recruiter/candidates/new").with(csrf())
                        .param("firstName", "Blocked")
                        .param("lastName", "User")
                        .param("email", "blocked@test.com")
                        .param("pipelineStage", "SOURCED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeClerk_cannotUpdateCandidate() throws Exception {
        mockMvc.perform(post("/recruiter/candidates/" + candidate.getId() + "/edit").with(csrf())
                        .param("firstName", "Blocked")
                        .param("lastName", "Update")
                        .param("email", "blocked@test.com")
                        .param("pipelineStage", "SCREENING"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void dispatchSupervisor_cannotBulkTag() throws Exception {
        mockMvc.perform(post("/recruiter/candidates/bulk-tag").with(csrf())
                        .param("candidateIds", candidate.getId().toString())
                        .param("tag", "infiltrate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanCreateCandidate() throws Exception {
        mockMvc.perform(post("/recruiter/candidates/new").with(csrf())
                        .param("firstName", "Admin")
                        .param("lastName", "CreatedCandidate")
                        .param("email", "admin.cand@test.com")
                        .param("pipelineStage", "SOURCED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/candidates"));
    }

    @Test
    void unauthenticated_cannotCreateCandidate() throws Exception {
        mockMvc.perform(post("/recruiter/candidates/new").with(csrf())
                        .param("firstName", "Anon")
                        .param("lastName", "User")
                        .param("email", "anon@test.com")
                        .param("pipelineStage", "SOURCED"))
                .andExpect(status().is3xxRedirection());
    }
}
