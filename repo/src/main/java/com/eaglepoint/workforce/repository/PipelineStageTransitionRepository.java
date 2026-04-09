package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.PipelineStageTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PipelineStageTransitionRepository extends JpaRepository<PipelineStageTransition, Long> {

    List<PipelineStageTransition> findByBatchIdAndUndoneFalse(String batchId);

    List<PipelineStageTransition> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
}
