package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.entity.CollectorProfile;
import com.eaglepoint.workforce.entity.DispatchAssignment;
import com.eaglepoint.workforce.entity.SiteProfile;
import com.eaglepoint.workforce.enums.CollectorStatus;
import com.eaglepoint.workforce.enums.DispatchMode;
import com.eaglepoint.workforce.enums.DispatchStatus;
import com.eaglepoint.workforce.repository.CollectorProfileRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DispatchAssignmentActionsCoverageTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SiteProfileRepository siteRepo;
    @Autowired
    private CollectorProfileRepository collectorRepo;
    @Autowired
    private DispatchAssignmentRepository assignmentRepo;

    private CollectorProfile collector;
    private SiteProfile siteOffer;
    private SiteProfile siteGrab;

    @BeforeEach
    void setup() {
        collector = new CollectorProfile();
        collector.setFirstName("Edge");
        collector.setLastName("Collector");
        collector.setEmployeeId("COL-E2E-" + System.nanoTime());
        collector.setStatus(CollectorStatus.AVAILABLE);
        collector.setMaxConcurrentJobs(5);
        collector = collectorRepo.save(collector);

        siteOffer = new SiteProfile();
        siteOffer.setName("Offer Site");
        siteOffer.setZone("Z1");
        siteOffer.setCapacityLimit(10);
        siteOffer.setCurrentOccupancy(0);
        siteOffer.setDispatchMode(DispatchMode.ASSIGNED_ORDER);
        siteOffer.setActive(true);
        siteOffer = siteRepo.save(siteOffer);

        siteGrab = new SiteProfile();
        siteGrab.setName("Grab Site");
        siteGrab.setZone("Z2");
        siteGrab.setCapacityLimit(10);
        siteGrab.setCurrentOccupancy(0);
        siteGrab.setDispatchMode(DispatchMode.GRAB_ORDER);
        siteGrab.setActive(true);
        siteGrab = siteRepo.save(siteGrab);
    }

    @Test
    @WithMockUser(username = "dispatch", roles = "DISPATCH_SUPERVISOR")
    void offer_accept_happyPath() throws Exception {
        DispatchAssignment a = pendingAssignment(siteOffer);
        mockMvc.perform(post("/dispatch/assignments/" + a.getId() + "/offer").with(csrf())
                        .param("collectorId", collector.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/assignments/" + a.getId()));

        mockMvc.perform(post("/dispatch/assignments/" + a.getId() + "/accept").with(csrf())
                        .param("collectorId", collector.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/assignments/" + a.getId()));
    }

    @Test
    @WithMockUser(username = "dispatch", roles = "DISPATCH_SUPERVISOR")
    void offer_decline_happyPath() throws Exception {
        DispatchAssignment a = pendingAssignment(siteOffer);
        mockMvc.perform(post("/dispatch/assignments/" + a.getId() + "/offer").with(csrf())
                        .param("collectorId", collector.getId().toString()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/dispatch/assignments/" + a.getId() + "/decline").with(csrf())
                        .param("collectorId", collector.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/assignments/" + a.getId()));
    }

    @Test
    @WithMockUser(username = "dispatch", roles = "DISPATCH_SUPERVISOR")
    void grab_pendingAssignment() throws Exception {
        DispatchAssignment a = pendingAssignment(siteGrab);
        mockMvc.perform(post("/dispatch/assignments/" + a.getId() + "/grab").with(csrf())
                        .param("collectorId", collector.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/assignments/" + a.getId()));
    }

    private DispatchAssignment pendingAssignment(SiteProfile site) {
        DispatchAssignment a = new DispatchAssignment();
        a.setSite(site);
        a.setTitle("Action Test");
        a.setStatus(DispatchStatus.PENDING);
        a.setDispatchMode(site.getDispatchMode());
        return assignmentRepo.save(a);
    }
}
