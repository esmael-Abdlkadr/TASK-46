package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.entity.CandidateProfile;
import java.util.ArrayList;
import java.util.List;

public class MatchResult {

    private CandidateProfile candidate;
    private double matchScore;
    private List<String> matchedRationale = new ArrayList<>();
    private List<String> unmatchedRationale = new ArrayList<>();
    private boolean meetsAllRequired;

    public MatchResult(CandidateProfile candidate) {
        this.candidate = candidate;
    }

    public CandidateProfile getCandidate() { return candidate; }
    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }
    public List<String> getMatchedRationale() { return matchedRationale; }
    public List<String> getUnmatchedRationale() { return unmatchedRationale; }
    public boolean isMeetsAllRequired() { return meetsAllRequired; }
    public void setMeetsAllRequired(boolean meetsAllRequired) { this.meetsAllRequired = meetsAllRequired; }

    public String getFullRationale() {
        StringBuilder sb = new StringBuilder();
        for (String r : matchedRationale) {
            sb.append("[MET] ").append(r).append("\n");
        }
        for (String r : unmatchedRationale) {
            sb.append("[NOT MET] ").append(r).append("\n");
        }
        return sb.toString().trim();
    }

    public int getMatchScorePercent() {
        return (int) Math.round(matchScore * 100);
    }
}
