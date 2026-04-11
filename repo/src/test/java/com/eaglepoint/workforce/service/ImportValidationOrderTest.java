package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.ImportJob;
import com.eaglepoint.workforce.enums.ImportStatus;
import com.eaglepoint.workforce.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that import header validation runs during VALIDATING phase (before IMPORTING).
 * Invalid headers should cause FAILED status, never IMPORTING.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportValidationOrderTest {

    @Autowired private ImportService importService;
    @Autowired private ImportJobRepository importJobRepo;

    @Test
    void invalidHeader_failsDuringValidating_neverReachesImporting() throws Exception {
        String csvContent = "BadColumn,AnotherBad\nval1,val2";
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad_headers.csv", "text/csv", csvContent.getBytes());

        ImportJob job = importService.prepareImport(file, "departments", 1L);
        assertEquals(ImportStatus.QUEUED, job.getStatus());

        // Execute synchronously (in test context @Async is disabled)
        importService.executeImport(job.getId());

        // Reload to verify final status
        ImportJob result = importJobRepo.findById(job.getId()).orElseThrow();
        assertEquals(ImportStatus.FAILED, result.getStatus(),
                "Invalid headers should cause FAILED status");
        assertTrue(result.getErrorReport().contains("Missing required column"),
                "Error report should mention missing columns, got: " + result.getErrorReport());
    }

    @Test
    void validHeader_reachesCompletedOrCompletedWithErrors() throws Exception {
        String csvContent = "Code,Name\nDEPT1,Test Department";
        MockMultipartFile file = new MockMultipartFile(
                "file", "valid_import.csv", "text/csv", csvContent.getBytes());

        ImportJob job = importService.prepareImport(file, "departments", 1L);
        importService.executeImport(job.getId());

        ImportJob result = importJobRepo.findById(job.getId()).orElseThrow();
        assertTrue(result.getStatus() == ImportStatus.COMPLETED
                        || result.getStatus() == ImportStatus.COMPLETED_WITH_ERRORS,
                "Valid headers should reach COMPLETED, got: " + result.getStatus());
    }

    @Test
    void unknownImportType_failsDuringValidating() throws Exception {
        String csvContent = "Code,Name\nX1,Test";
        MockMultipartFile file = new MockMultipartFile(
                "file", "unknown_type.csv", "text/csv", csvContent.getBytes());

        ImportJob job = importService.prepareImport(file, "badtype", 1L);
        importService.executeImport(job.getId());

        ImportJob result = importJobRepo.findById(job.getId()).orElseThrow();
        assertEquals(ImportStatus.FAILED, result.getStatus());
        assertTrue(result.getErrorReport().contains("Unknown import type"));
    }

    @Test
    void missingRequiredField_rowError_reportsRowNumber() throws Exception {
        String csvContent = "Code,Name\n,Missing Code";
        MockMultipartFile file = new MockMultipartFile(
                "file", "missing_field.csv", "text/csv", csvContent.getBytes());

        ImportJob job = importService.prepareImport(file, "departments", 1L);
        importService.executeImport(job.getId());

        ImportJob result = importJobRepo.findById(job.getId()).orElseThrow();
        assertEquals(ImportStatus.COMPLETED_WITH_ERRORS, result.getStatus());
        assertTrue(result.getErrorReport().contains("Row"),
                "Error report should contain row numbers");
    }
}
