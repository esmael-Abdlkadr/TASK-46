package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "search_snapshots")
public class SearchSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "search_criteria_json", nullable = false, columnDefinition = "TEXT")
    private String searchCriteriaJson;

    @Column(name = "job_profile_id")
    private Long jobProfileId;

    @OneToMany(mappedBy = "searchSnapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("matchScore DESC")
    private List<SearchSnapshotResult> results = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public String getSearchCriteriaJson() { return searchCriteriaJson; }
    public void setSearchCriteriaJson(String searchCriteriaJson) { this.searchCriteriaJson = searchCriteriaJson; }
    public Long getJobProfileId() { return jobProfileId; }
    public void setJobProfileId(Long jobProfileId) { this.jobProfileId = jobProfileId; }
    public List<SearchSnapshotResult> getResults() { return results; }
    public void setResults(List<SearchSnapshotResult> results) { this.results = results; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
