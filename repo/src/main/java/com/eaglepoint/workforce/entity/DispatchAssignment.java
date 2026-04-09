package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.DispatchMode;
import com.eaglepoint.workforce.enums.DispatchStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_assignments", indexes = {
    @Index(name = "idx_dispatch_status", columnList = "status"),
    @Index(name = "idx_dispatch_collector", columnList = "collector_id"),
    @Index(name = "idx_dispatch_site", columnList = "site_id"),
    @Index(name = "idx_dispatch_expires", columnList = "acceptance_expires_at")
})
public class DispatchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteProfile site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id")
    private CollectorProfile collector;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispatchStatus status = DispatchStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_mode", nullable = false, length = 20)
    private DispatchMode dispatchMode;

    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;

    @Column(name = "acceptance_expires_at")
    private LocalDateTime acceptanceExpiresAt;

    @Column(name = "offer_count", nullable = false)
    private int offerCount = 0;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isAcceptanceExpired() {
        return acceptanceExpiresAt != null && LocalDateTime.now().isAfter(acceptanceExpiresAt);
    }

    public long getAcceptanceRemainingSeconds() {
        if (acceptanceExpiresAt == null) return 0;
        long remaining = java.time.Duration.between(LocalDateTime.now(), acceptanceExpiresAt).getSeconds();
        return Math.max(0, remaining);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SiteProfile getSite() { return site; }
    public void setSite(SiteProfile site) { this.site = site; }
    public CollectorProfile getCollector() { return collector; }
    public void setCollector(CollectorProfile collector) { this.collector = collector; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DispatchStatus getStatus() { return status; }
    public void setStatus(DispatchStatus status) { this.status = status; }
    public DispatchMode getDispatchMode() { return dispatchMode; }
    public void setDispatchMode(DispatchMode dispatchMode) { this.dispatchMode = dispatchMode; }
    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(LocalDateTime scheduledStart) { this.scheduledStart = scheduledStart; }
    public LocalDateTime getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(LocalDateTime scheduledEnd) { this.scheduledEnd = scheduledEnd; }
    public LocalDateTime getAcceptanceExpiresAt() { return acceptanceExpiresAt; }
    public void setAcceptanceExpiresAt(LocalDateTime acceptanceExpiresAt) { this.acceptanceExpiresAt = acceptanceExpiresAt; }
    public int getOfferCount() { return offerCount; }
    public void setOfferCount(int offerCount) { this.offerCount = offerCount; }
    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long assignedBy) { this.assignedBy = assignedBy; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
