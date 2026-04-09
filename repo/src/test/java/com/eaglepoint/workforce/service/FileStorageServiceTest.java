package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.StoredFile;
import com.eaglepoint.workforce.repository.StoredFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileStorageServiceTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Test
    void storeFileWithFingerprint() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test content for storage".getBytes());

        StoredFile stored = fileStorageService.store(file, "test", 1L);

        assertNotNull(stored.getId());
        assertEquals("test.txt", stored.getOriginalName());
        assertNotNull(stored.getFingerprint());
        assertEquals(64, stored.getFingerprint().length());
        assertEquals("test", stored.getCategory());
    }

    @Test
    void duplicateFileReturnsSameRecord() throws IOException {
        byte[] content = "duplicate test content".getBytes();
        MockMultipartFile file1 = new MockMultipartFile("file", "f1.txt", "text/plain", content);
        MockMultipartFile file2 = new MockMultipartFile("file", "f2.txt", "text/plain", content);

        StoredFile stored1 = fileStorageService.store(file1, "test", 1L);
        StoredFile stored2 = fileStorageService.store(file2, "test", 1L);

        assertEquals(stored1.getId(), stored2.getId());
        assertEquals(stored1.getFingerprint(), stored2.getFingerprint());
    }

    @Test
    void verifyFingerprintIntegrity() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "verify.txt", "text/plain", "integrity check".getBytes());

        StoredFile stored = fileStorageService.store(file, "test", 1L);

        assertTrue(fileStorageService.verifyFingerprint(stored));
    }

    @Test
    void findByFingerprint() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lookup.txt", "text/plain", "lookup content".getBytes());

        StoredFile stored = fileStorageService.store(file, "docs", 1L);

        var found = fileStorageService.findByFingerprint(stored.getFingerprint());
        assertTrue(found.isPresent());
        assertEquals(stored.getId(), found.get().getId());
    }

    @Test
    void existsByFingerprint() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "exists.txt", "text/plain", "exists content".getBytes());

        StoredFile stored = fileStorageService.store(file, "test", 1L);

        assertTrue(fileStorageService.existsByFingerprint(stored.getFingerprint()));
        assertFalse(fileStorageService.existsByFingerprint("nonexistent"));
    }
}
