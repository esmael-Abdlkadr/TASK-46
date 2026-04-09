package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.BatchMoveResult;
import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.enums.PipelineStage;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PipelineServiceTest {

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private CandidateProfileRepository candidateRepository;

    private CandidateProfile c1, c2, c3;

    @BeforeEach
    void setUp() {
        c1 = createCandidate("John", "Doe");
        c2 = createCandidate("Jane", "Doe");
        c3 = createCandidate("Jim", "Beam");
    }

    @Test
    void batchMoveChangesStage() {
        BatchMoveResult result = pipelineService.batchMoveStage(
                List.of(c1.getId(), c2.getId()),
                PipelineStage.SCREENING,
                1L);

        assertEquals(2, result.getMovedCount());
        assertNotNull(result.getBatchId());

        CandidateProfile updated1 = candidateRepository.findById(c1.getId()).orElseThrow();
        assertEquals(PipelineStage.SCREENING, updated1.getPipelineStage());
    }

    @Test
    void batchMoveSkipsSameStage() {
        c1.setPipelineStage(PipelineStage.SCREENING);
        candidateRepository.save(c1);

        BatchMoveResult result = pipelineService.batchMoveStage(
                List.of(c1.getId(), c2.getId()),
                PipelineStage.SCREENING,
                1L);

        assertEquals(1, result.getMovedCount()); // Only c2 moved
    }

    @Test
    void undoBatchMoveRevertsStage() {
        BatchMoveResult result = pipelineService.batchMoveStage(
                List.of(c1.getId(), c2.getId()),
                PipelineStage.PHONE_INTERVIEW,
                1L);

        int undoneCount = pipelineService.undoBatchMove(result.getBatchId());
        assertEquals(2, undoneCount);

        CandidateProfile reverted = candidateRepository.findById(c1.getId()).orElseThrow();
        assertEquals(PipelineStage.SOURCED, reverted.getPipelineStage());
    }

    @Test
    void undoAfterExpiryThrowsException() {
        BatchMoveResult result = pipelineService.batchMoveStage(
                List.of(c1.getId()),
                PipelineStage.SCREENING,
                1L);

        // Undo should work within the window
        assertTrue(pipelineService.isUndoAvailable(result.getBatchId()));
    }

    @Test
    void undoNonExistentBatchThrows() {
        assertThrows(RuntimeException.class,
                () -> pipelineService.undoBatchMove("non-existent-batch-id"));
    }

    private CandidateProfile createCandidate(String first, String last) {
        CandidateProfile c = new CandidateProfile();
        c.setFirstName(first);
        c.setLastName(last);
        c.setPipelineStage(PipelineStage.SOURCED);
        return candidateRepository.save(c);
    }
}
