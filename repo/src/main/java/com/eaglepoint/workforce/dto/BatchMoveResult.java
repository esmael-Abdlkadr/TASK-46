package com.eaglepoint.workforce.dto;

import java.time.LocalDateTime;

public class BatchMoveResult {

    private String batchId;
    private int movedCount;
    private LocalDateTime undoDeadline;

    public BatchMoveResult(String batchId, int movedCount, LocalDateTime undoDeadline) {
        this.batchId = batchId;
        this.movedCount = movedCount;
        this.undoDeadline = undoDeadline;
    }

    public String getBatchId() { return batchId; }
    public int getMovedCount() { return movedCount; }
    public LocalDateTime getUndoDeadline() { return undoDeadline; }
}
