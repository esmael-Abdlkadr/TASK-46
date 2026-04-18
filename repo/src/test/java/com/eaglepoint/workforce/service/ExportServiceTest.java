package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.ExportJob;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.ExportStatus;
import com.eaglepoint.workforce.repository.ExportJobRepository;
import com.eaglepoint.workforce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExportServiceTest {

    @Autowired
    private ExportService exportService;
    @Autowired
    private ExportJobRepository exportJobRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void queueExport_persistsQueuedJobWithTypes() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        ExportJob job = exportService.queueExport(
                "Integration export", "departments", "csv", null, admin.getId());

        assertNotNull(job.getId());
        assertEquals(ExportStatus.QUEUED, job.getStatus());
        assertEquals("departments", job.getExportType());
        assertEquals("csv", job.getFileFormat());

        ExportJob loaded = exportJobRepository.findById(job.getId()).orElseThrow();
        assertEquals(admin.getId(), loaded.getCreatedBy());
    }

    @Test
    void executeExport_collectorsExcel_eventuallyCompletes() throws InterruptedException {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        ExportJob job = exportService.queueExport(
                "Collector dump", "collectors", "xlsx", null, admin.getId());

        exportService.executeExport(job.getId());

        ExportJob done = exportJobRepository.findById(job.getId()).orElseThrow();
        for (int i = 0; i < 80; i++) {
            done = exportJobRepository.findById(job.getId()).orElseThrow();
            if (done.getStatus() == ExportStatus.COMPLETED || done.getStatus() == ExportStatus.FAILED) {
                break;
            }
            Thread.sleep(50);
        }

        assertEquals(ExportStatus.COMPLETED, done.getStatus());
        assertNotNull(done.getFilePath());
        assertTrue(done.getRecordCount() >= 0);
    }
}
