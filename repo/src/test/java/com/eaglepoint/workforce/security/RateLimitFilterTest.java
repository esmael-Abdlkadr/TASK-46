package com.eaglepoint.workforce.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.rate-limit.requests-per-minute=5")
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void rateLimitReturns429WhenExceeded() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isTooManyRequests());
    }
}
