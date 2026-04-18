package com.eaglepoint.workforce.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FormImportExportMasterDataCoverageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void exportCreate_postsForm() throws Exception {
        mockMvc.perform(post("/exports/create").with(csrf())
                        .param("name", "Coverage Export")
                        .param("exportType", "CSV")
                        .param("fileFormat", "CSV"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/exports"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void importUpload_multipartCsv() throws Exception {
        String name = "imp-" + Long.toHexString(System.nanoTime()) + ".csv";
        MockMultipartFile file = new MockMultipartFile(
                "file", name, "text/csv", "Code,Name\nD1,Dept1\n".getBytes());
        mockMvc.perform(multipart("/imports/upload")
                        .file(file)
                        .param("importType", "departments")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/imports"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATOR")
    void masterdataClassesSave_postsForm() throws Exception {
        mockMvc.perform(post("/masterdata/classes/save").with(csrf())
                        .param("code", "CLS-" + Long.toHexString(System.nanoTime()))
                        .param("name", "Coverage Class"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/masterdata/classes"));
    }
}
