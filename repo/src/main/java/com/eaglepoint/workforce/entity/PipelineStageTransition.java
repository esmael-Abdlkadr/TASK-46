package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.PipelineStage;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pipeline_stage_transitions", indexes = {
    @Index(name = "idx_transition_batch", columnList = "batch_id"),
    @Index(name = "idx_transition_undo", columnList = "undone, undo_deadline")
})
public class PipelineStageTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", nullable = false, length = 30)
    private PipelineStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false, length = 30)
    private PipelineStage toStage;

    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(nullable = false)
    private boolean undone = false;

    @Column(name = "undo_deadline", nullable = false)
    private LocalDateTime undoDeadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public PipelineStage getFromStage() { return fromStage; }
    public void setFromStage(PipelineStage fromStage) { this.fromStage = fromStage; }
    public PipelineStage getToStage() { return toStage; }
    public void setToStage(PipelineStage toStage) { this.toStage = toStage; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public Long getPerformedBy() { return performedBy; }
    public void setPerformedBy(Long performedBy) { this.performedBy = performedBy; }
    public boolean isUndone() { return undone; }
    public void setUndone(boolean undone) { this.undone = undone; }
    public LocalDateTime getUndoDeadline() { return undoDeadline; }
    public void setUndoDeadline(LocalDateTime undoDeadline) { this.undoDeadline = undoDeadline; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
