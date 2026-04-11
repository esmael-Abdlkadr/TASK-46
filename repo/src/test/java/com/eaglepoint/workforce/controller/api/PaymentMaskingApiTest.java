package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.*;
import com.eaglepoint.workforce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests masking behavior by role for payment API endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentMaskingApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PaymentTransactionRepository paymentRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;

    private PaymentTransaction testPayment;

    @BeforeEach
    void setup() {
        // Ensure users exist
        ensureUser("adminUser", RoleName.ADMINISTRATOR);
        ensureUser("finClerk", RoleName.FINANCE_CLERK);

        testPayment = new PaymentTransaction();
        testPayment.setIdempotencyKey("mask-test-key-" + System.nanoTime());
        testPayment.setReferenceNumber("MASK-001");
        testPayment.setAmount(new BigDecimal("500.00"));
        testPayment.setRefundedAmount(BigDecimal.ZERO);
        testPayment.setChannel(PaymentChannel.CHECK);
        testPayment.setStatus(PaymentStatus.RECORDED);
        testPayment.setPayerName("John Smith");
        testPayment.setCheckNumber("CHK-12345678");
        testPayment.setCardLastFour("9876");
        testPayment.setRecordedBy(1L);
        testPayment.setRecordedByUsername("admin");
        testPayment.setTransactionDate(LocalDateTime.now());
        testPayment = paymentRepo.save(testPayment);
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMINISTRATOR")
    void admin_seesFullPayerName() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/payments/" + testPayment.getId()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("John Smith"), "Admin should see full payer name");
        assertTrue(body.contains("CHK-12345678"), "Admin should see full check number");
        assertTrue(body.contains("9876"), "Admin should see card last four");
    }

    @Test
    @WithMockUser(username = "finClerk", roles = "FINANCE_CLERK")
    void financeClerk_seesMaskedPayerName() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/payments/" + testPayment.getId()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("John Smith"), "Finance clerk should NOT see full payer name");
        assertFalse(body.contains("CHK-12345678"), "Finance clerk should NOT see full check number");
        assertTrue(body.contains("*"), "Response should contain masked asterisks");
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMINISTRATOR")
    void admin_listPayments_seesFullFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("John Smith"), "Admin list should show full payer name");
    }

    @Test
    @WithMockUser(username = "finClerk", roles = "FINANCE_CLERK")
    void financeClerk_listPayments_seesMaskedFields() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("John Smith"), "Finance clerk list should NOT show full payer name");
    }

    private void ensureUser(String username, RoleName roleName) {
        Role role = roleRepo.findByName(roleName).orElseGet(() -> {
            Role r = new Role();
            r.setName(roleName);
            return roleRepo.save(r);
        });
        userRepo.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName(username);
            u.setEmail(username + "@test.com");
            u.setEnabled(true);
            u.getRoles().add(role);
            return userRepo.save(u);
        });
    }
}
