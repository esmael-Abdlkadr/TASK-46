package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.enums.PipelineStage;
import java.util.List;

public class BatchMoveRequest {

    private List<Long> candidateIds;
    private PipelineStage targetStage;

    public List<Long> getCandidateIds() { return candidateIds; }
    public void setCandidateIds(List<Long> candidateIds) { this.candidateIds = candidateIds; }
    public PipelineStage getTargetStage() { return targetStage; }
    public void setTargetStage(PipelineStage targetStage) { this.targetStage = targetStage; }
}
