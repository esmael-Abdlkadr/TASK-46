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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests duplicate payment submission/idempotency at controller/API level.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentApiIdempotencyTest {

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
        userRepo.findByUsername("finUser").orElseGet(() -> {
            User u = new User();
            u.setUsername("finUser");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Fin User");
            u.setEmail("fin@test.com");
            u.setEnabled(true);
            u.getRoles().add(finRole);
            return userRepo.save(u);
        });
    }

    @Test
    @WithMockUser(username = "finUser", roles = "FINANCE_CLERK")
    void duplicateSubmission_withClientKey_returnsSameId() throws Exception {
        String json = """
                {
                    "idempotencyKey": "client-key-dup-test-001",
                    "referenceNumber": "PAY-DUP-001",
                    "amount": 100.00,
                    "channel": "CASH"
                }
                """;

        MvcResult r1 = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult r2 = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn();

        // Both should return the same payment ID
        String body1 = r1.getResponse().getContentAsString();
        String body2 = r2.getResponse().getContentAsString();
        // Extract "id": from JSON
        assertTrue(body1.contains("\"id\""));
        assertEquals(extractId(body1), extractId(body2),
                "Duplicate submission must return the same payment record");
    }

    @Test
    @WithMockUser(username = "finUser", roles = "FINANCE_CLERK")
    void duplicateSubmission_withoutClientKey_deterministicKey() throws Exception {
        String json = """
                {
                    "referenceNumber": "PAY-AUTO-001",
                    "amount": 250.50,
                    "channel": "CHECK",
                    "checkNumber": "CHK-999"
                }
                """;

        MvcResult r1 = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult r2 = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(extractId(r1.getResponse().getContentAsString()),
                extractId(r2.getResponse().getContentAsString()),
                "Auto-generated key must be deterministic");
    }

    @Test
    @WithMockUser(username = "finUser", roles = "FINANCE_CLERK")
    void payment_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "finUser", roles = "FINANCE_CLERK")
    void payment_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "referenceNumber": "NEG-001",
                                    "amount": -50,
                                    "channel": "CASH"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private String extractId(String json) {
        // Simple extraction of "id":N from JSON
        int idx = json.indexOf("\"id\"");
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx);
        int end = json.indexOf(',', colon);
        if (end < 0) end = json.indexOf('}', colon);
        return json.substring(colon + 1, end).trim();
    }
}
