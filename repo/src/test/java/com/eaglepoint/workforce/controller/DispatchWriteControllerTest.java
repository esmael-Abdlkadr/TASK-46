package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.entity.DispatchAssignment;
import com.eaglepoint.workforce.entity.SiteProfile;
import com.eaglepoint.workforce.enums.DispatchMode;
import com.eaglepoint.workforce.enums.DispatchStatus;
import com.eaglepoint.workforce.repository.DispatchAssignmentRepository;
import com.eaglepoint.workforce.repository.SiteProfileRepository;
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
 * Covers write (POST) endpoints in /dispatch/assignments: create, complete, cancel.
 * Verifies RBAC isolation and successful supervisor operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DispatchWriteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SiteProfileRepository siteRepo;
    @Autowired private DispatchAssignmentRepository assignmentRepo;

    private SiteProfile site;
    private DispatchAssignment assignment;

    @BeforeEach
    void setup() {
        SiteProfile s = new SiteProfile();
        s.setName("Test Site Alpha");
        s.setZone("Zone A");
        s.setCapacityLimit(10);
        s.setCurrentOccupancy(0);
        s.setDispatchMode(DispatchMode.GRAB_ORDER);
        s.setActive(true);
        site = siteRepo.save(s);

        DispatchAssignment a = new DispatchAssignment();
        a.setSite(site);
        a.setTitle("Test Assignment");
        a.setStatus(DispatchStatus.PENDING);
        a.setDispatchMode(DispatchMode.GRAB_ORDER);
        assignment = assignmentRepo.save(a);
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "DISPATCH_SUPERVISOR")
    void supervisorCanCreateAssignment() throws Exception {
        mockMvc.perform(post("/dispatch/assignments/new").with(csrf())
                        .param("siteId", site.getId().toString())
                        .param("title", "New Dispatch Assignment")
                        .param("description", "Test description"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/assignments"));
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "DISPATCH_SUPERVISOR")
    void supervisorCanViewAssignmentDetail() throws Exception {
        mockMvc.perform(get("/dispatch/assignments/" + assignment.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "DISPATCH_SUPERVISOR")
    void supervisorCanCompleteAssignment() throws Exception {
        mockMvc.perform(post("/dispatch/assignments/" + assignment.getId() + "/complete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/assignments/" + assignment.getId()));
    }

    @Test
    @WithMockUser(username = "supervisor", roles = "DISPATCH_SUPERVISOR")
    void supervisorCanCancelAssignment() throws Exception {
        mockMvc.perform(post("/dispatch/assignments/" + assignment.getId() + "/cancel").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/assignments/" + assignment.getId()));
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiter_cannotCreateAssignment() throws Exception {
        mockMvc.perform(post("/dispatch/assignments/new").with(csrf())
                        .param("siteId", site.getId().toString())
                        .param("title", "Blocked"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeClerk_cannotCompleteAssignment() throws Exception {
        mockMvc.perform(post("/dispatch/assignments/" + assignment.getId() + "/complete").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeClerk_cannotCancelAssignment() throws Exception {
        mockMvc.perform(post("/dispatch/assignments/" + assignment.getId() + "/cancel").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_cannotCreateAssignment() throws Exception {
        mockMvc.perform(post("/dispatch/assignments/new").with(csrf())
                        .param("siteId", site.getId().toString())
                        .param("title", "Blocked"))
                .andExpect(status().is3xxRedirection());
    }
}
