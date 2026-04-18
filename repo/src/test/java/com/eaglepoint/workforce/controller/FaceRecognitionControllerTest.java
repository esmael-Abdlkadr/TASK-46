package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FaceRecognitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

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

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void enroll_postsMultipartAndRedirects() throws Exception {
        long uid = userRepository.findByUsername("admin").orElseThrow().getId();
        MockMultipartFile img = new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[]{2, 3, 4});
        mockMvc.perform(multipart("/admin/face-recognition/enroll")
                        .file(img)
                        .param("userId", Long.toString(uid))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/face-recognition"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void verify_postsMultipartAndRedirects() throws Exception {
        long uid = userRepository.findByUsername("admin").orElseThrow().getId();
        MockMultipartFile img = new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[]{5, 6});
        mockMvc.perform(multipart("/admin/face-recognition/verify")
                        .file(img)
                        .param("userId", Long.toString(uid))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/face-recognition"));
    }
}
