package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.BatchMoveResult;
import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.entity.PipelineStageTransition;
import com.eaglepoint.workforce.enums.PipelineStage;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
import com.eaglepoint.workforce.repository.PipelineStageTransitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PipelineService {

    private static final int UNDO_WINDOW_SECONDS = 60;

    private final CandidateProfileRepository candidateRepository;
    private final PipelineStageTransitionRepository transitionRepository;

    public PipelineService(CandidateProfileRepository candidateRepository,
                           PipelineStageTransitionRepository transitionRepository) {
        this.candidateRepository = candidateRepository;
        this.transitionRepository = transitionRepository;
    }

    @Transactional
    public BatchMoveResult batchMoveStage(List<Long> candidateIds, PipelineStage targetStage,
                                           Long performedBy) {
        String batchId = UUID.randomUUID().toString();
        LocalDateTime undoDeadline = LocalDateTime.now().plusSeconds(UNDO_WINDOW_SECONDS);

        List<CandidateProfile> candidates = candidateRepository.findAllById(candidateIds);
        int movedCount = 0;

        for (CandidateProfile candidate : candidates) {
            PipelineStage fromStage = candidate.getPipelineStage();
            if (fromStage == targetStage) {
                continue;
            }

            PipelineStageTransition transition = new PipelineStageTransition();
            transition.setCandidateId(candidate.getId());
            transition.setFromStage(fromStage);
            transition.setToStage(targetStage);
            transition.setBatchId(batchId);
            transition.setPerformedBy(performedBy);
            transition.setUndoDeadline(undoDeadline);
            transitionRepository.save(transition);

            candidate.setPipelineStage(targetStage);
            candidateRepository.save(candidate);
            movedCount++;
        }

        return new BatchMoveResult(batchId, movedCount, undoDeadline);
    }

    @Transactional
    public int undoBatchMove(String batchId) {
        List<PipelineStageTransition> transitions =
                transitionRepository.findByBatchIdAndUndoneFalse(batchId);

        if (transitions.isEmpty()) {
            throw new RuntimeException("No transitions found for batch: " + batchId);
        }

        LocalDateTime now = LocalDateTime.now();
        PipelineStageTransition first = transitions.get(0);
        if (now.isAfter(first.getUndoDeadline())) {
            throw new RuntimeException("Undo window has expired for batch: " + batchId);
        }

        int undoneCount = 0;
        for (PipelineStageTransition transition : transitions) {
            CandidateProfile candidate = candidateRepository.findById(transition.getCandidateId())
                    .orElse(null);
            if (candidate != null) {
                candidate.setPipelineStage(transition.getFromStage());
                candidateRepository.save(candidate);
            }
            transition.setUndone(true);
            transitionRepository.save(transition);
            undoneCount++;
        }

        return undoneCount;
    }

    @Transactional(readOnly = true)
    public boolean isUndoAvailable(String batchId) {
        List<PipelineStageTransition> transitions =
                transitionRepository.findByBatchIdAndUndoneFalse(batchId);
        if (transitions.isEmpty()) {
            return false;
        }
        return LocalDateTime.now().isBefore(transitions.get(0).getUndoDeadline());
    }
}
