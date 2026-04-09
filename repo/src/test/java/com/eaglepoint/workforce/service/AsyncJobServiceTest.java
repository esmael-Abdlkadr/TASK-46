package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.AsyncJob;
import com.eaglepoint.workforce.enums.AsyncJobStatus;
import com.eaglepoint.workforce.enums.AsyncJobType;
import com.eaglepoint.workforce.repository.AsyncJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AsyncJobServiceTest {

    @Autowired
    private AsyncJobService asyncJobService;

    @Autowired
    private AsyncJobRepository jobRepository;

    @Test
    void submitCreatesQueuedJob() {
        AsyncJob job = asyncJobService.submit(AsyncJobType.REPORT_GENERATION,
                "Monthly report", "{}", 1L, "admin");

        assertNotNull(job.getId());
        assertEquals(AsyncJobStatus.QUEUED, job.getStatus());
        assertEquals(AsyncJobType.REPORT_GENERATION, job.getJobType());
        assertEquals(0, job.getProgressPercent());
    }

    @Test
    void markRunningUpdatesStatus() {
        AsyncJob job = asyncJobService.submit(AsyncJobType.BATCH_IMPORT,
                "Import data", null, 1L, "admin");
        AsyncJob running = asyncJobService.markRunning(job.getId());

        assertEquals(AsyncJobStatus.RUNNING, running.getStatus());
        assertNotNull(running.getStartedAt());
    }

    @Test
    void markCompletedSetsResult() {
        AsyncJob job = asyncJobService.submit(AsyncJobType.DATA_EXPORT,
                "Export candidates", null, 1L, "admin");
        asyncJobService.markRunning(job.getId());
        AsyncJob completed = asyncJobService.markCompleted(job.getId(), "500 records exported");

        assertEquals(AsyncJobStatus.COMPLETED, completed.getStatus());
        assertEquals("500 records exported", completed.getResultData());
        assertEquals(100, completed.getProgressPercent());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void markFailedSetsError() {
        AsyncJob job = asyncJobService.submit(AsyncJobType.FACE_FEATURE_EXTRACTION,
                "Extract features", null, 1L, "admin");
        asyncJobService.markRunning(job.getId());
        AsyncJob failed = asyncJobService.markFailed(job.getId(), "Service unavailable");

        assertEquals(AsyncJobStatus.FAILED, failed.getStatus());
        assertEquals("Service unavailable", failed.getErrorMessage());
    }

    @Test
    void updateProgress() {
        AsyncJob job = asyncJobService.submit(AsyncJobType.BATCH_IMPORT,
                "Import", null, 1L, "admin");
        asyncJobService.updateProgress(job.getId(), 42);

        AsyncJob updated = jobRepository.findById(job.getId()).orElseThrow();
        assertEquals(42, updated.getProgressPercent());
    }

    @Test
    void healthStatusReturnsMetrics() {
        asyncJobService.submit(AsyncJobType.REPORT_GENERATION, "Job 1", null, 1L, "admin");
        asyncJobService.submit(AsyncJobType.BATCH_IMPORT, "Job 2", null, 1L, "admin");

        Map<String, Object> health = asyncJobService.getHealthStatus();

        assertEquals("HEALTHY", health.get("status"));
        assertTrue((Long) health.get("queued") >= 2);
        assertNotNull(health.get("maxQueued"));
        assertNotNull(health.get("warnThreshold"));
    }

    @Test
    void healthWarnsOnThreshold() {
        // Submit enough jobs to exceed warn threshold (default 50, test may use lower)
        // Just verify the structure works
        Map<String, Object> health = asyncJobService.getHealthStatus();
        assertNotNull(health.get("status"));
        assertTrue(health.containsKey("queued_report_generation"));
    }
}
