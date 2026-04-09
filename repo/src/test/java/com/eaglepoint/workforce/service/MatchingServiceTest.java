package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.MatchResult;
import com.eaglepoint.workforce.dto.MatchSearchRequest;
import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.*;
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
class MatchingServiceTest {

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private CandidateProfileRepository candidateRepository;

    @Autowired
    private JobProfileRepository jobProfileRepository;

    private CandidateProfile candidate1;
    private CandidateProfile candidate2;
    private JobProfile jobProfile;

    @BeforeEach
    void setUp() {
        // Candidate 1: Strong Java developer
        candidate1 = new CandidateProfile();
        candidate1.setFirstName("Alice");
        candidate1.setLastName("Smith");
        candidate1.setLocation("New York");
        candidate1.setYearsOfExperience(8);
        candidate1.setCurrentTitle("Senior Developer");
        candidate1.setEducationLevel("Bachelor's");
        candidate1.getSkills().add(new CandidateSkill(candidate1, "Java", 7, SkillProficiency.EXPERT));
        candidate1.getSkills().add(new CandidateSkill(candidate1, "Spring Boot", 5, SkillProficiency.ADVANCED));
        candidate1.getSkills().add(new CandidateSkill(candidate1, "SQL", 6, SkillProficiency.ADVANCED));
        candidateRepository.save(candidate1);

        // Candidate 2: Junior developer
        candidate2 = new CandidateProfile();
        candidate2.setFirstName("Bob");
        candidate2.setLastName("Jones");
        candidate2.setLocation("New York");
        candidate2.setYearsOfExperience(2);
        candidate2.setCurrentTitle("Junior Developer");
        candidate2.setEducationLevel("Bachelor's");
        candidate2.getSkills().add(new CandidateSkill(candidate2, "Java", 2, SkillProficiency.INTERMEDIATE));
        candidate2.getSkills().add(new CandidateSkill(candidate2, "Python", 1, SkillProficiency.BEGINNER));
        candidateRepository.save(candidate2);

        // Job profile requiring Java 5+ and Spring Boot 3+
        jobProfile = new JobProfile();
        jobProfile.setTitle("Senior Java Developer");
        jobProfile.setLocation("New York");
        jobProfile.setMinYearsExperience(5);
        jobProfile.getCriteria().add(new JobProfileCriterion(jobProfile, "Java", 5, CriterionType.REQUIRED, 2.0));
        jobProfile.getCriteria().add(new JobProfileCriterion(jobProfile, "Spring Boot", 3, CriterionType.REQUIRED, 1.5));
        jobProfile.getCriteria().add(new JobProfileCriterion(jobProfile, "Docker", null, CriterionType.PREFERRED, 0.5));
        jobProfileRepository.save(jobProfile);
    }

    @Test
    void matchCandidatesWithSkillCriteria() {
        MatchSearchRequest request = new MatchSearchRequest();
        request.setOperator(BooleanOperator.AND);

        MatchSearchRequest.SkillCriterion javaCriterion = new MatchSearchRequest.SkillCriterion();
        javaCriterion.setSkillName("Java");
        javaCriterion.setMinYears(5);
        javaCriterion.setWeight(2.0);
        javaCriterion.setRequired(true);

        MatchSearchRequest.SkillCriterion springCriterion = new MatchSearchRequest.SkillCriterion();
        springCriterion.setSkillName("Spring Boot");
        springCriterion.setMinYears(3);
        springCriterion.setWeight(1.5);
        springCriterion.setRequired(true);

        request.setSkillCriteria(List.of(javaCriterion, springCriterion));

        List<MatchResult> results = matchingService.matchCandidates(request);

        assertFalse(results.isEmpty());
        // Alice should rank first with higher score
        MatchResult topResult = results.get(0);
        assertEquals("Alice", topResult.getCandidate().getFirstName());
        assertTrue(topResult.getMatchScore() > 0);
        assertTrue(topResult.isMeetsAllRequired());
    }

    @Test
    void matchAgainstJobProfileReturnsRankedResults() {
        List<MatchResult> results = matchingService.matchAgainstJobProfile(jobProfile.getId());

        assertFalse(results.isEmpty());
        // Alice meets all criteria; Bob has only partial match
        MatchResult alice = results.stream()
                .filter(r -> r.getCandidate().getFirstName().equals("Alice"))
                .findFirst().orElse(null);

        assertNotNull(alice);
        assertTrue(alice.getMatchScore() > 0.5);
        assertFalse(alice.getMatchedRationale().isEmpty());
    }

    @Test
    void matchRationaleIncludesExplainableDetail() {
        MatchSearchRequest request = new MatchSearchRequest();
        MatchSearchRequest.SkillCriterion sc = new MatchSearchRequest.SkillCriterion();
        sc.setSkillName("Java");
        sc.setMinYears(5);
        sc.setWeight(1.0);
        sc.setRequired(true);
        request.setSkillCriteria(List.of(sc));

        List<MatchResult> results = matchingService.matchCandidates(request);

        MatchResult aliceResult = results.stream()
                .filter(r -> r.getCandidate().getFirstName().equals("Alice"))
                .findFirst().orElseThrow();

        // Should have "Java 5+ years required met" style rationale
        assertTrue(aliceResult.getMatchedRationale().stream()
                .anyMatch(r -> r.contains("Java") && r.contains("5+") && r.contains("met")));
    }

    @Test
    void locationFilterNarrowsResults() {
        MatchSearchRequest request = new MatchSearchRequest();
        request.setLocationFilter("Chicago");

        List<MatchResult> results = matchingService.matchCandidates(request);
        assertTrue(results.isEmpty()); // No candidates in Chicago
    }

    @Test
    void experienceFilterWorks() {
        MatchSearchRequest request = new MatchSearchRequest();
        request.setMinYearsExperience(5);

        List<MatchResult> results = matchingService.matchCandidates(request);
        // Only Alice has 8 years
        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).getCandidate().getFirstName());
    }

    @Test
    void unmatchedSkillsShowExplanation() {
        MatchSearchRequest request = new MatchSearchRequest();
        MatchSearchRequest.SkillCriterion sc = new MatchSearchRequest.SkillCriterion();
        sc.setSkillName("Kubernetes");
        sc.setMinYears(3);
        sc.setWeight(1.0);
        sc.setRequired(true);
        request.setSkillCriteria(List.of(sc));

        List<MatchResult> results = matchingService.matchCandidates(request);

        for (MatchResult r : results) {
            assertFalse(r.getUnmatchedRationale().isEmpty());
            assertTrue(r.getUnmatchedRationale().stream()
                    .anyMatch(s -> s.contains("Kubernetes") && s.contains("not met")));
        }
    }
}
