package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.ExportStatus;
import com.eaglepoint.workforce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests object-level authorization: cross-user access (same role, different owner),
 * tenant/user isolation for list + detail + download paths,
 * and REST 401/403/404 + validation error responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ObjectLevelAuthzTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ExportJobRepository exportJobRepo;
    @Autowired private ImportJobRepository importJobRepo;
    @Autowired private SavedSearchRepository savedSearchRepo;
    @Autowired private TalentPoolRepository talentPoolRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;

    private User userA;
    private User userB;

    @BeforeEach
    void setup() {
        // Create two users with RECRUITER role for cross-user tests
        Role recruiterRole = roleRepo.findByName(
                com.eaglepoint.workforce.enums.RoleName.RECRUITER).orElseGet(() -> {
            Role r = new Role();
            r.setName(com.eaglepoint.workforce.enums.RoleName.RECRUITER);
            return roleRepo.save(r);
        });

        userA = userRepo.findByUsername("testUserA").orElseGet(() -> {
            User u = new User();
            u.setUsername("testUserA");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("User A");
            u.setEmail("a@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });

        userB = userRepo.findByUsername("testUserB").orElseGet(() -> {
            User u = new User();
            u.setUsername("testUserB");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("User B");
            u.setEmail("b@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });
    }

    // --- Export authz tests ---

    @Test
    @WithMockUser(username = "testUserA", roles = "RECRUITER")
    void exportDetail_ownerCanAccess() throws Exception {
        ExportJob job = new ExportJob();
        job.setName("A's export");
        job.setExportType("candidates");
        job.setFileFormat("csv");
        job.setCreatedBy(userA.getId());
        job = exportJobRepo.save(job);

        mockMvc.perform(get("/api/v1/exports/" + job.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testUserB", roles = "RECRUITER")
    void exportDetail_nonOwnerDenied() throws Exception {
        ExportJob job = new ExportJob();
        job.setName("A's export");
        job.setExportType("candidates");
        job.setFileFormat("csv");
        job.setCreatedBy(userA.getId());
        job = exportJobRepo.save(job);

        mockMvc.perform(get("/api/v1/exports/" + job.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testUserA", roles = "RECRUITER")
    void exportList_ownerScopedOnly() throws Exception {
        ExportJob jobA = new ExportJob();
        jobA.setName("A's export");
        jobA.setExportType("candidates");
        jobA.setFileFormat("csv");
        jobA.setCreatedBy(userA.getId());
        exportJobRepo.save(jobA);

        ExportJob jobB = new ExportJob();
        jobB.setName("B's export");
        jobB.setExportType("candidates");
        jobB.setFileFormat("csv");
        jobB.setCreatedBy(userB.getId());
        exportJobRepo.save(jobB);

        mockMvc.perform(get("/api/v1/exports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == \"A's export\")]").exists())
                .andExpect(jsonPath("$[?(@.name == \"B's export\")]").doesNotExist());
    }

    // --- Import authz tests ---

    @Test
    @WithMockUser(username = "testUserA", roles = "RECRUITER")
    void importDetail_ownerCanAccess() throws Exception {
        ImportJob job = new ImportJob();
        job.setOriginalFileName("test.csv");
        job.setImportType("departments");
        job.setFileFingerprint("fp-owner-test-a");
        job.setCreatedBy(userA.getId());
        job = importJobRepo.save(job);

        mockMvc.perform(get("/api/v1/imports/" + job.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testUserB", roles = "RECRUITER")
    void importDetail_nonOwnerDenied() throws Exception {
        ImportJob job = new ImportJob();
        job.setOriginalFileName("test.csv");
        job.setImportType("departments");
        job.setFileFingerprint("fp-owner-test-b");
        job.setCreatedBy(userA.getId());
        job = importJobRepo.save(job);

        mockMvc.perform(get("/api/v1/imports/" + job.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testUserA", roles = "RECRUITER")
    void importList_ownerScopedOnly() throws Exception {
        ImportJob jobA = new ImportJob();
        jobA.setOriginalFileName("a.csv");
        jobA.setImportType("departments");
        jobA.setFileFingerprint("fp-list-a");
        jobA.setCreatedBy(userA.getId());
        importJobRepo.save(jobA);

        ImportJob jobB = new ImportJob();
        jobB.setOriginalFileName("b.csv");
        jobB.setImportType("departments");
        jobB.setFileFingerprint("fp-list-b");
        jobB.setCreatedBy(userB.getId());
        importJobRepo.save(jobB);

        mockMvc.perform(get("/api/v1/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.originalFileName == 'a.csv')]").exists())
                .andExpect(jsonPath("$[?(@.originalFileName == 'b.csv')]").doesNotExist());
    }

    // --- Saved search authz tests ---

    @Test
    @WithMockUser(username = "testUserA", roles = "RECRUITER")
    void savedSearchDetail_ownerCanAccess() throws Exception {
        SavedSearch ss = new SavedSearch();
        ss.setName("A's search");
        ss.setCreatedBy(userA.getId());
        ss.setSearchCriteriaJson("{}");
        ss = savedSearchRepo.save(ss);

        mockMvc.perform(get("/api/v1/search/saved/" + ss.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testUserB", roles = "RECRUITER")
    void savedSearchDetail_nonOwnerDenied() throws Exception {
        SavedSearch ss = new SavedSearch();
        ss.setName("A's search");
        ss.setCreatedBy(userA.getId());
        ss.setSearchCriteriaJson("{}");
        ss = savedSearchRepo.save(ss);

        mockMvc.perform(get("/api/v1/search/saved/" + ss.getId()))
                .andExpect(status().isNotFound());
    }

    // --- REST 404 for nonexistent resources ---

    @Test
    @WithMockUser(username = "testUserA", roles = "RECRUITER")
    void exportDetail_nonexistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/exports/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testUserA", roles = "RECRUITER")
    void importDetail_nonexistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/imports/99999"))
                .andExpect(status().isNotFound());
    }

    // --- REST validation error tests ---

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void payment_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void export_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/exports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void export_create_validRequest_returnsJob() throws Exception {
        mockMvc.perform(post("/api/v1/exports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mock Export Job\",\"exportType\":\"departments\",\"fileFormat\":\"csv\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mock Export Job"))
                .andExpect(jsonPath("$.exportType").value("departments"));
    }

    // --- Unauthenticated tests ---

    @Test
    void unauthenticated_paymentApi_redirected() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void unauthenticated_exportApi_redirected() throws Exception {
        mockMvc.perform(get("/api/v1/exports"))
                .andExpect(status().is3xxRedirection());
    }

    // --- Role-based access denial ---

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiter_paymentApiAccess_denied() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            status == 403 || result.getResponse().getContentAsString().toLowerCase().contains("denied"),
                            "Recruiter should be denied payment API, got status=" + status);
                });
    }
}
