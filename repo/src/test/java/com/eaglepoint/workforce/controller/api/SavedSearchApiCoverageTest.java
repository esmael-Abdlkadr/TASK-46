package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.entity.Role;
import com.eaglepoint.workforce.entity.SavedSearch;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.RoleRepository;
import com.eaglepoint.workforce.repository.SavedSearchRepository;
import com.eaglepoint.workforce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers GET /api/v1/search/saved: list is owner-scoped for non-admin, admin sees all,
 * unauthenticated request redirects.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SavedSearchApiCoverageTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;
    @Autowired private SavedSearchRepository savedSearchRepo;

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

        userA = userRepo.findByUsername("ssUserA").orElseGet(() -> {
            User u = new User();
            u.setUsername("ssUserA");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("SS User A");
            u.setEmail("ssa@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });

        userB = userRepo.findByUsername("ssUserB").orElseGet(() -> {
            User u = new User();
            u.setUsername("ssUserB");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("SS User B");
            u.setEmail("ssb@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });

        userRepo.findByUsername("ssAdminUser").orElseGet(() -> {
            User u = new User();
            u.setUsername("ssAdminUser");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("SS Admin User");
            u.setEmail("ssadmin@test.com");
            u.setEnabled(true);
            u.setRoles(Set.of(adminRole));
            return userRepo.save(u);
        });

        SavedSearch ssA = new SavedSearch();
        ssA.setName("UserA-Search");
        ssA.setCreatedBy(userA.getId());
        ssA.setSearchCriteriaJson("{\"keyword\":\"java\"}");
        savedSearchRepo.save(ssA);

        SavedSearch ssB = new SavedSearch();
        ssB.setName("UserB-Search");
        ssB.setCreatedBy(userB.getId());
        ssB.setSearchCriteriaJson("{\"keyword\":\"python\"}");
        savedSearchRepo.save(ssB);
    }

    @Test
    @WithMockUser(username = "ssUserA", roles = "RECRUITER")
    void listSavedSearches_returnsOnlyOwnSearches() throws Exception {
        mockMvc.perform(get("/api/v1/search/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'UserA-Search')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'UserB-Search')]").doesNotExist());
    }

    @Test
    @WithMockUser(username = "ssUserB", roles = "RECRUITER")
    void listSavedSearches_differentUser_seesOnlyOwnSearches() throws Exception {
        mockMvc.perform(get("/api/v1/search/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'UserB-Search')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'UserA-Search')]").doesNotExist());
    }

    @Test
    @WithMockUser(username = "ssAdminUser", roles = "ADMINISTRATOR")
    void listSavedSearches_admin_seesAllSearches() throws Exception {
        mockMvc.perform(get("/api/v1/search/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'UserA-Search')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'UserB-Search')]").exists());
    }

    @Test
    void unauthenticated_listSavedSearches_redirects() throws Exception {
        mockMvc.perform(get("/api/v1/search/saved"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "ssUserA", roles = "RECRUITER")
    void listSavedSearches_returnsJsonArray() throws Exception {
        mockMvc.perform(get("/api/v1/search/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
