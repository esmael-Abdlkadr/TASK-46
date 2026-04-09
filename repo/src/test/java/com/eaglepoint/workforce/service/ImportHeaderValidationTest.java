package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.ImportJob;
import com.eaglepoint.workforce.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportHeaderValidationTest {

    @Autowired private ImportService importService;

    @Test
    void validHeaders_accepted() throws Exception {
        String csv = "Code,Name,Head Name\nDEPT-1,Engineering,John";
        MockMultipartFile file = new MockMultipartFile("file", "valid.csv", "text/csv", csv.getBytes());
        ImportJob job = importService.prepareImport(file, "departments", 1L);
        assertNotNull(job.getId());
    }

    @Test
    void missingRequiredColumn_rejected() {
        // "Code" is required for departments but absent in this header
        String[] badHeader = {"WrongCol", "Name"};
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                importService.validateHeaderSchema("departments", badHeader));
        assertTrue(ex.getMessage().contains("Missing required column"),
                "Should report missing Code column, got: " + ex.getMessage());
    }

    @Test
    void coursesValidHeaders() throws Exception {
        String csv = "Code,Name,Credit Hours\nCRS-1,Java,40";
        MockMultipartFile file = new MockMultipartFile("file", "courses.csv", "text/csv", csv.getBytes());
        ImportJob job = importService.prepareImport(file, "courses", 1L);
        assertNotNull(job.getId());
    }
}
