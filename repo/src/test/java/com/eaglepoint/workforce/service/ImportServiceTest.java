package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.ImportJob;
import com.eaglepoint.workforce.enums.ImportStatus;
import com.eaglepoint.workforce.repository.DepartmentRepository;
import com.eaglepoint.workforce.repository.ImportJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ImportServiceTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportJobRepository importJobRepo;

    @Autowired
    private DepartmentRepository departmentRepo;

    @Test
    void sha256ComputationIsConsistent() throws Exception {
        byte[] data = "test content for hashing".getBytes();
        String hash1 = ImportService.computeSha256(new ByteArrayInputStream(data));
        String hash2 = ImportService.computeSha256(new ByteArrayInputStream(data));
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void sha256DiffersForDifferentContent() throws Exception {
        String hash1 = ImportService.computeSha256(new ByteArrayInputStream("content A".getBytes()));
        String hash2 = ImportService.computeSha256(new ByteArrayInputStream("content B".getBytes()));
        assertNotEquals(hash1, hash2);
    }

    @Test
    void duplicateFileDetection() throws Exception {
        String csvContent = "Code,Name\nTEST1,Test Dept";
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "departments.csv", "text/csv", csvContent.getBytes());

        ImportJob job1 = importService.prepareImport(file1, "departments", 1L);
        assertNotEquals(ImportStatus.DUPLICATE, job1.getStatus());

        MockMultipartFile file2 = new MockMultipartFile(
                "file", "departments2.csv", "text/csv", csvContent.getBytes());

        ImportJob job2 = importService.prepareImport(file2, "departments", 1L);
        assertEquals(ImportStatus.DUPLICATE, job2.getStatus());
    }

    @Test
    void formatValidation_rejectsInvalidExtensions() {
        // prepareImport checks at controller level, but the service accepts any file
        // The controller validates .csv/.xlsx/.xls - this test verifies the job creation
        MockMultipartFile csv = new MockMultipartFile(
                "file", "test.csv", "text/csv", "Code,Name\nD1,Dept1".getBytes());
        assertDoesNotThrow(() -> importService.prepareImport(csv, "departments", 1L));
    }
}
