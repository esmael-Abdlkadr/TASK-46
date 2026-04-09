package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.MatchSearchRequest;
import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.CriterionType;
import com.eaglepoint.workforce.enums.SkillProficiency;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
import com.eaglepoint.workforce.repository.JobProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchSnapshotServiceTest {

    @Autowired
    private SearchSnapshotService snapshotService;

    @Autowired
    private CandidateProfileRepository candidateRepository;

    @Autowired
    private JobProfileRepository jobProfileRepository;

    @BeforeEach
    void setUp() {
        CandidateProfile candidate = new CandidateProfile();
        candidate.setFirstName("Snapshot");
        candidate.setLastName("Test");
        candidate.setYearsOfExperience(5);
        candidate.getSkills().add(new CandidateSkill(candidate, "React", 4, SkillProficiency.ADVANCED));
        candidateRepository.save(candidate);
    }

    @Test
    void snapshotCapturesResultsAndRationale() {
        MatchSearchRequest request = new MatchSearchRequest();
        MatchSearchRequest.SkillCriterion sc = new MatchSearchRequest.SkillCriterion();
        sc.setSkillName("React");
        sc.setMinYears(2);
        sc.setWeight(1.0);
        sc.setRequired(true);
        request.setSkillCriteria(List.of(sc));

        SearchSnapshot snapshot = snapshotService.createSnapshot("Test Snapshot", request, 1L);

        assertNotNull(snapshot.getId());
        assertEquals("Test Snapshot", snapshot.getName());
        assertFalse(snapshot.getResults().isEmpty());

        SearchSnapshotResult result = snapshot.getResults().get(0);
        assertEquals("Snapshot Test", result.getCandidateName());
        assertTrue(result.getMatchScore() > 0);
        assertNotNull(result.getMatchRationale());
        assertTrue(result.getMatchRationale().contains("React"));
        assertNotNull(result.getCandidateSnapshotJson());
    }

    @Test
    void snapshotFromJobProfileWorks() {
        JobProfile jp = new JobProfile();
        jp.setTitle("React Developer");
        jp.getCriteria().add(new JobProfileCriterion(jp, "React", 2, CriterionType.REQUIRED, 1.0));
        jobProfileRepository.save(jp);

        SearchSnapshot snapshot = snapshotService.createSnapshotFromJobProfile(
                "JP Snapshot", jp.getId(), 1L);

        assertNotNull(snapshot.getId());
        assertFalse(snapshot.getResults().isEmpty());
    }

    @Test
    void snapshotRationaleRemainsStableAfterProfileChange() {
        MatchSearchRequest request = new MatchSearchRequest();
        MatchSearchRequest.SkillCriterion sc = new MatchSearchRequest.SkillCriterion();
        sc.setSkillName("React");
        sc.setMinYears(2);
        sc.setWeight(1.0);
        sc.setRequired(true);
        request.setSkillCriteria(List.of(sc));

        SearchSnapshot snapshot = snapshotService.createSnapshot("Stable Test", request, 1L);
        String originalRationale = snapshot.getResults().get(0).getMatchRationale();
        double originalScore = snapshot.getResults().get(0).getMatchScore();

        // Modify the candidate after snapshot
        CandidateProfile candidate = candidateRepository.findAll().stream()
                .filter(c -> "Snapshot".equals(c.getFirstName()))
                .findFirst().orElseThrow();
        candidate.getSkills().clear();
        candidateRepository.save(candidate);

        // Reload snapshot -- rationale should be unchanged
        SearchSnapshot reloaded = snapshotService.findByIdWithResults(snapshot.getId()).orElseThrow();
        assertEquals(originalRationale, reloaded.getResults().get(0).getMatchRationale());
        assertEquals(originalScore, reloaded.getResults().get(0).getMatchScore());
    }
}
