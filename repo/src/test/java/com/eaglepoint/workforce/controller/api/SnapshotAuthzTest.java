package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests object-level authorization for search snapshots:
 * - Owner can access
 * - Non-owner gets 404 (no existence leakage)
 * - Admin can access any snapshot
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SnapshotAuthzTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SearchSnapshotRepository snapshotRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;

    private User userA;
    private User userB;

    @BeforeEach
    void setup() {
        Role recruiterRole = roleRepo.findByName(RoleName.RECRUITER).orElseGet(() -> {
            Role r = new Role();
            r.setName(RoleName.RECRUITER);
            return roleRepo.save(r);
        });
        Role adminRole = roleRepo.findByName(RoleName.ADMINISTRATOR).orElseGet(() -> {
            Role r = new Role();
            r.setName(RoleName.ADMINISTRATOR);
            return roleRepo.save(r);
        });

        userA = userRepo.findByUsername("snapUserA").orElseGet(() -> {
            User u = new User();
            u.setUsername("snapUserA");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Snap User A");
            u.setEmail("snapa@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });

        userB = userRepo.findByUsername("snapUserB").orElseGet(() -> {
            User u = new User();
            u.setUsername("snapUserB");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Snap User B");
            u.setEmail("snapb@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });

        userRepo.findByUsername("snapAdmin").orElseGet(() -> {
            User u = new User();
            u.setUsername("snapAdmin");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Snap Admin");
            u.setEmail("snapadmin@test.com");
            u.setEnabled(true);
            u.getRoles().add(adminRole);
            return userRepo.save(u);
        });
    }

    @Test
    @WithMockUser(username = "snapUserA", roles = "RECRUITER")
    void snapshotDetail_ownerCanAccess() throws Exception {
        SearchSnapshot snap = new SearchSnapshot();
        snap.setName("A's snapshot");
        snap.setCreatedBy(userA.getId());
        snap.setSearchCriteriaJson("{}");
        snap = snapshotRepo.save(snap);

        mockMvc.perform(get("/recruiter/search/snapshot/" + snap.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "snapUserB", roles = "RECRUITER")
    void snapshotDetail_nonOwnerDenied() throws Exception {
        SearchSnapshot snap = new SearchSnapshot();
        snap.setName("A's snapshot");
        snap.setCreatedBy(userA.getId());
        snap.setSearchCriteriaJson("{}");
        snap = snapshotRepo.save(snap);

        mockMvc.perform(get("/recruiter/search/snapshot/" + snap.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "snapAdmin", roles = "ADMINISTRATOR")
    void snapshotDetail_adminCanAccessAny() throws Exception {
        SearchSnapshot snap = new SearchSnapshot();
        snap.setName("A's snapshot");
        snap.setCreatedBy(userA.getId());
        snap.setSearchCriteriaJson("{}");
        snap = snapshotRepo.save(snap);

        mockMvc.perform(get("/recruiter/search/snapshot/" + snap.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "snapUserA", roles = "RECRUITER")
    void snapshotDetail_nonexistent_returns404() throws Exception {
        mockMvc.perform(get("/recruiter/search/snapshot/99999"))
                .andExpect(status().isNotFound());
    }
}
