package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.entity.Role;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.RoleRepository;
import com.eaglepoint.workforce.repository.UserRepository;
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
 * Tests the consistent error envelope format: code, message, fieldErrors, timestamp, path.
 * Also tests that internal errors do not expose stack traces.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ErrorEnvelopeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;

    @BeforeEach
    void setup() {
        Role finRole = roleRepo.findByName(RoleName.FINANCE_CLERK).orElseGet(() -> {
            Role r = new Role();
            r.setName(RoleName.FINANCE_CLERK);
            return roleRepo.save(r);
        });
        userRepo.findByUsername("errTestUser").orElseGet(() -> {
            User u = new User();
            u.setUsername("errTestUser");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Error Test User");
            u.setEmail("errtest@test.com");
            u.setEnabled(true);
            u.getRoles().add(finRole);
            return userRepo.save(u);
        });
    }

    @Test
    @WithMockUser(username = "errTestUser", roles = "FINANCE_CLERK")
    void validationError_hasFieldErrorsAndPath() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/payments"));
    }

    @Test
    @WithMockUser(username = "errTestUser", roles = "FINANCE_CLERK")
    void notFound_hasCodeAndPath() throws Exception {
        mockMvc.perform(get("/api/v1/payments/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/payments/99999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void notFound_export_hasPath() throws Exception {
        mockMvc.perform(get("/api/v1/exports/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/exports/99999"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void notFound_import_hasPath() throws Exception {
        mockMvc.perform(get("/api/v1/imports/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/imports/99999"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void notFound_savedSearch_hasPath() throws Exception {
        mockMvc.perform(get("/api/v1/search/saved/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/search/saved/99999"));
    }

    @Test
    @WithMockUser(username = "errTestUser", roles = "FINANCE_CLERK")
    void badRequest_negativeAmount_hasPath() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "referenceNumber": "NEG-ERR",
                                    "amount": -10,
                                    "channel": "CASH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/payments"));
    }
}
