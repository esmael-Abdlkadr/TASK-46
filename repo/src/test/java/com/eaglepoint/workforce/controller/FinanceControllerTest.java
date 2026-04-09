package com.eaglepoint.workforce.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCanAccessPayments() throws Exception {
        mockMvc.perform(get("/finance/payments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCanAccessPaymentForm() throws Exception {
        mockMvc.perform(get("/finance/payments/new"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCanAccessBankFiles() throws Exception {
        mockMvc.perform(get("/finance/bank-files"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCanAccessReconciliation() throws Exception {
        mockMvc.perform(get("/finance/reconciliation"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCanAccessSettlement() throws Exception {
        mockMvc.perform(get("/finance/settlement"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCannotAccessFinance() throws Exception {
        mockMvc.perform(get("/finance/payments"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/finance/bank-files"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/finance/reconciliation"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessFinance() throws Exception {
        mockMvc.perform(get("/finance/payments"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/finance/reconciliation"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedCannotAccessFinance() throws Exception {
        mockMvc.perform(get("/finance/payments"))
                .andExpect(status().is3xxRedirection());
    }
}
