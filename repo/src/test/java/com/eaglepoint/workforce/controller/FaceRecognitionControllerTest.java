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
class FaceRecognitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessFaceRecognition() throws Exception {
        mockMvc.perform(get("/admin/face-recognition"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessJobQueue() throws Exception {
        mockMvc.perform(get("/admin/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCannotAccessFaceRecognition() throws Exception {
        mockMvc.perform(get("/admin/face-recognition"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeCannotAccessJobQueue() throws Exception {
        mockMvc.perform(get("/admin/jobs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotAccess() throws Exception {
        mockMvc.perform(get("/admin/face-recognition"))
                .andExpect(status().is3xxRedirection());
    }
}
