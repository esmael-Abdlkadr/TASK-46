package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.entity.Role;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.PaymentChannel;
import com.eaglepoint.workforce.enums.PaymentStatus;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.PaymentTransactionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers POST /api/v1/payments/{id}/refund: happy path, validation errors,
 * not-found, excess-amount, and RBAC denial. Asserts full error envelope contract.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentRefundApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;
    @Autowired private PaymentTransactionRepository paymentRepo;

    private PaymentTransaction payment;

    @BeforeEach
    void setup() {
        Role finRole = roleRepo.findByName(RoleName.FINANCE_CLERK).orElseGet(() -> {
            Role r = new Role();
            r.setName(RoleName.FINANCE_CLERK);
            return roleRepo.save(r);
        });
        User finUser = userRepo.findByUsername("refundApiUser").orElseGet(() -> {
            User u = new User();
            u.setUsername("refundApiUser");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Refund API User");
            u.setEmail("refundapi@test.com");
            u.setEnabled(true);
            u.getRoles().add(finRole);
            return userRepo.save(u);
        });

        PaymentTransaction p = new PaymentTransaction();
        p.setIdempotencyKey("idem-refund-api-test-001");
        p.setReferenceNumber("REFUND-API-001");
        p.setAmount(new BigDecimal("200.00"));
        p.setRefundedAmount(BigDecimal.ZERO);
        p.setChannel(PaymentChannel.CASH);
        p.setStatus(PaymentStatus.RECORDED);
        p.setLocation("Test HQ");
        p.setRecordedBy(finUser.getId());
        p.setRecordedByUsername(finUser.getUsername());
        p.setTransactionDate(LocalDateTime.now());
        payment = paymentRepo.save(p);
    }

    @Test
    @WithMockUser(username = "refundApiUser", roles = "FINANCE_CLERK")
    void happyPath_partialRefund_returnsPaymentWithUpdatedRefundedAmount() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50.00,\"reason\":\"Partial refund test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId()))
                .andExpect(jsonPath("$.referenceNumber").value("REFUND-API-001"))
                .andExpect(jsonPath("$.refundedAmount").value(50.00));
    }

    @Test
    @WithMockUser(username = "refundApiUser", roles = "FINANCE_CLERK")
    void validationError_missingAmount_returns400WithFullEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"missing amount field\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/payments/" + payment.getId() + "/refund"));
    }

    @Test
    @WithMockUser(username = "refundApiUser", roles = "FINANCE_CLERK")
    void validationError_missingReason_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.path").value("/api/v1/payments/" + payment.getId() + "/refund"));
    }

    @Test
    @WithMockUser(username = "refundApiUser", roles = "FINANCE_CLERK")
    void notFound_nonExistentPayment_returns404WithEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/payments/99999/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"reason\":\"not found test\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/payments/99999/refund"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(username = "refundApiUser", roles = "FINANCE_CLERK")
    void excessRefund_beyondPaymentAmount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":9999.00,\"reason\":\"Excess refund\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/payments/" + payment.getId() + "/refund"));
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void rbac_recruiterCannotCallRefundEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/payments/" + payment.getId() + "/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"reason\":\"RBAC test\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 403 || result.getResponse()
                                    .getContentAsString().toLowerCase().contains("denied"),
                            "Recruiter must be denied refund endpoint, got status=" + status);
                });
    }
}
