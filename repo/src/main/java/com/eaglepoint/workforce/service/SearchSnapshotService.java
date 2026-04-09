package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.MatchResult;
import com.eaglepoint.workforce.dto.MatchSearchRequest;
import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.entity.SearchSnapshot;
import com.eaglepoint.workforce.entity.SearchSnapshotResult;
import com.eaglepoint.workforce.repository.SearchSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SearchSnapshotService {

    private final SearchSnapshotRepository snapshotRepository;
    private final MatchingService matchingService;
    private final ObjectMapper objectMapper;

    public SearchSnapshotService(SearchSnapshotRepository snapshotRepository,
                                  MatchingService matchingService,
                                  ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.matchingService = matchingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SearchSnapshot createSnapshot(String name, MatchSearchRequest request,
                                          Long createdBy) {
        List<MatchResult> results = matchingService.matchCandidates(request);

        SearchSnapshot snapshot = new SearchSnapshot();
        snapshot.setName(name);
        snapshot.setCreatedBy(createdBy);
        snapshot.setJobProfileId(request.getJobProfileId());

        try {
            snapshot.setSearchCriteriaJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize search criteria", e);
        }

        for (MatchResult mr : results) {
            CandidateProfile candidate = mr.getCandidate();
            String candidateJson = serializeCandidateSnapshot(candidate);

            SearchSnapshotResult ssr = new SearchSnapshotResult(
                    snapshot,
                    candidate.getId(),
                    candidate.getFullName(),
                    mr.getMatchScore(),
                    mr.getFullRationale(),
                    candidateJson
            );
            snapshot.getResults().add(ssr);
        }

        return snapshotRepository.save(snapshot);
    }

    @Transactional
    public SearchSnapshot createSnapshotFromJobProfile(String name, Long jobProfileId, Long createdBy) {
        List<MatchResult> results = matchingService.matchAgainstJobProfile(jobProfileId);

        MatchSearchRequest request = new MatchSearchRequest();
        request.setJobProfileId(jobProfileId);

        SearchSnapshot snapshot = new SearchSnapshot();
        snapshot.setName(name);
        snapshot.setCreatedBy(createdBy);
        snapshot.setJobProfileId(jobProfileId);

        try {
            snapshot.setSearchCriteriaJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize search criteria", e);
        }

        for (MatchResult mr : results) {
            CandidateProfile candidate = mr.getCandidate();
            String candidateJson = serializeCandidateSnapshot(candidate);

            SearchSnapshotResult ssr = new SearchSnapshotResult(
                    snapshot,
                    candidate.getId(),
                    candidate.getFullName(),
                    mr.getMatchScore(),
                    mr.getFullRationale(),
                    candidateJson
            );
            snapshot.getResults().add(ssr);
        }

        return snapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public List<SearchSnapshot> findByCreator(Long userId) {
        return snapshotRepository.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<SearchSnapshot> findByIdWithResults(Long id) {
        return snapshotRepository.findByIdWithResults(id);
    }

    private String serializeCandidateSnapshot(CandidateProfile candidate) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", candidate.getId());
        snapshot.put("name", candidate.getFullName());
        snapshot.put("title", candidate.getCurrentTitle());
        snapshot.put("employer", candidate.getCurrentEmployer());
        snapshot.put("yearsExperience", candidate.getYearsOfExperience());
        snapshot.put("location", candidate.getLocation());
        snapshot.put("education", candidate.getEducationLevel());

        List<Map<String, Object>> skills = new ArrayList<>();
        for (var skill : candidate.getSkills()) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", skill.getSkillName());
            s.put("years", skill.getYearsExperience());
            s.put("proficiency", skill.getProficiency() != null ? skill.getProficiency().name() : null);
            skills.add(s);
        }
        snapshot.put("skills", skills);

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
