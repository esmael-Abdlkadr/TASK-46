package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_offer_log", indexes = {
    @Index(name = "idx_offer_assignment", columnList = "assignment_id"),
    @Index(name = "idx_offer_collector", columnList = "collector_id")
})
public class DispatchOfferLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "collector_id", nullable = false)
    private Long collectorId;

    @Column(name = "collector_name", length = 200)
    private String collectorName;

    @Column(nullable = false, length = 20)
    private String outcome;

    @Column(name = "offered_at", nullable = false)
    private LocalDateTime offeredAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public DispatchOfferLog() {}

    public DispatchOfferLog(Long assignmentId, Long collectorId, String collectorName,
                             String outcome, LocalDateTime offeredAt) {
        this.assignmentId = assignmentId;
        this.collectorId = collectorId;
        this.collectorName = collectorName;
        this.outcome = outcome;
        this.offeredAt = offeredAt;
    }

    public Long getId() { return id; }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public Long getCollectorId() { return collectorId; }
    public void setCollectorId(Long collectorId) { this.collectorId = collectorId; }
    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public LocalDateTime getOfferedAt() { return offeredAt; }
    public void setOfferedAt(LocalDateTime offeredAt) { this.offeredAt = offeredAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}
