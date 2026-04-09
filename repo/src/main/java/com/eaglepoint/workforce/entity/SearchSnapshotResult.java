package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "search_snapshot_results")
public class SearchSnapshotResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private SearchSnapshot searchSnapshot;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "candidate_name", nullable = false, length = 200)
    private String candidateName;

    @Column(name = "match_score", nullable = false)
    private Double matchScore;

    @Column(name = "match_rationale", nullable = false, columnDefinition = "TEXT")
    private String matchRationale;

    @Column(name = "candidate_snapshot_json", columnDefinition = "TEXT")
    private String candidateSnapshotJson;

    public SearchSnapshotResult() {}

    public SearchSnapshotResult(SearchSnapshot searchSnapshot, Long candidateId, String candidateName,
                                 Double matchScore, String matchRationale, String candidateSnapshotJson) {
        this.searchSnapshot = searchSnapshot;
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.matchScore = matchScore;
        this.matchRationale = matchRationale;
        this.candidateSnapshotJson = candidateSnapshotJson;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SearchSnapshot getSearchSnapshot() { return searchSnapshot; }
    public void setSearchSnapshot(SearchSnapshot searchSnapshot) { this.searchSnapshot = searchSnapshot; }
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public Double getMatchScore() { return matchScore; }
    public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }
    public String getMatchRationale() { return matchRationale; }
    public void setMatchRationale(String matchRationale) { this.matchRationale = matchRationale; }
    public String getCandidateSnapshotJson() { return candidateSnapshotJson; }
    public void setCandidateSnapshotJson(String candidateSnapshotJson) { this.candidateSnapshotJson = candidateSnapshotJson; }
}
