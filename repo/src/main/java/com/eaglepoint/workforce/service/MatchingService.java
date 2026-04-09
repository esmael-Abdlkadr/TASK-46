package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.MatchResult;
import com.eaglepoint.workforce.dto.MatchSearchRequest;
import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.BooleanOperator;
import com.eaglepoint.workforce.enums.CriterionType;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
import com.eaglepoint.workforce.repository.JobProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private final CandidateProfileRepository candidateRepository;
    private final JobProfileRepository jobProfileRepository;

    public MatchingService(CandidateProfileRepository candidateRepository,
                           JobProfileRepository jobProfileRepository) {
        this.candidateRepository = candidateRepository;
        this.jobProfileRepository = jobProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<MatchResult> matchCandidates(MatchSearchRequest request) {
        List<CandidateProfile> candidates = loadCandidates(request);
        List<MatchSearchRequest.SkillCriterion> criteria = buildCriteria(request);

        return candidates.stream()
                .map(c -> scoreCandidate(c, criteria, request))
                .filter(r -> r.getMatchScore() > 0)
                .sorted(Comparator.comparingDouble(MatchResult::getMatchScore).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchResult> matchAgainstJobProfile(Long jobProfileId) {
        JobProfile jp = jobProfileRepository.findByIdWithCriteria(jobProfileId)
                .orElseThrow(() -> new RuntimeException("Job profile not found: " + jobProfileId));

        MatchSearchRequest request = new MatchSearchRequest();
        request.setJobProfileId(jobProfileId);
        request.setOperator(BooleanOperator.AND);

        if (jp.getMinYearsExperience() != null) {
            request.setMinYearsExperience(jp.getMinYearsExperience());
        }
        if (jp.getLocation() != null && !jp.getLocation().isBlank()) {
            request.setLocationFilter(jp.getLocation());
        }

        List<MatchSearchRequest.SkillCriterion> criteria = new ArrayList<>();
        for (JobProfileCriterion jpc : jp.getCriteria()) {
            MatchSearchRequest.SkillCriterion sc = new MatchSearchRequest.SkillCriterion();
            sc.setSkillName(jpc.getSkillName());
            sc.setMinYears(jpc.getMinYears());
            sc.setWeight(jpc.getWeight());
            sc.setRequired(jpc.getCriterionType() == CriterionType.REQUIRED);
            criteria.add(sc);
        }
        request.setSkillCriteria(criteria);

        return matchCandidates(request);
    }

    private List<CandidateProfile> loadCandidates(MatchSearchRequest request) {
        List<String> skillNames = request.getSkillCriteria().stream()
                .map(MatchSearchRequest.SkillCriterion::getSkillName)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());

        List<CandidateProfile> candidates;
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            candidates = candidateRepository.searchByKeyword(request.getKeyword());
        } else if (!skillNames.isEmpty()) {
            candidates = candidateRepository.findBySkillNames(skillNames);
        } else {
            candidates = candidateRepository.findAllWithSkills();
        }

        return candidates.stream()
                .filter(c -> matchesBasicFilters(c, request))
                .collect(Collectors.toList());
    }

    private boolean matchesBasicFilters(CandidateProfile candidate, MatchSearchRequest request) {
        if (request.getLocationFilter() != null && !request.getLocationFilter().isBlank()) {
            if (candidate.getLocation() == null ||
                !candidate.getLocation().toLowerCase().contains(request.getLocationFilter().toLowerCase())) {
                return false;
            }
        }
        if (request.getMinYearsExperience() != null && candidate.getYearsOfExperience() != null) {
            if (candidate.getYearsOfExperience() < request.getMinYearsExperience()) {
                return false;
            }
        }
        if (request.getMaxYearsExperience() != null && candidate.getYearsOfExperience() != null) {
            if (candidate.getYearsOfExperience() > request.getMaxYearsExperience()) {
                return false;
            }
        }
        if (request.getEducationLevel() != null && !request.getEducationLevel().isBlank()) {
            if (candidate.getEducationLevel() == null ||
                !candidate.getEducationLevel().toLowerCase().contains(request.getEducationLevel().toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private List<MatchSearchRequest.SkillCriterion> buildCriteria(MatchSearchRequest request) {
        if (request.getSkillCriteria() != null && !request.getSkillCriteria().isEmpty()) {
            return request.getSkillCriteria();
        }
        return Collections.emptyList();
    }

    private MatchResult scoreCandidate(CandidateProfile candidate,
                                        List<MatchSearchRequest.SkillCriterion> criteria,
                                        MatchSearchRequest request) {
        MatchResult result = new MatchResult(candidate);

        if (criteria.isEmpty()) {
            result.setMatchScore(1.0);
            result.setMeetsAllRequired(true);
            result.getMatchedRationale().add("Candidate matches basic filters");
            return result;
        }

        Map<String, CandidateSkill> candidateSkillMap = new HashMap<>();
        for (CandidateSkill cs : candidate.getSkills()) {
            candidateSkillMap.put(cs.getSkillName().toLowerCase(), cs);
        }

        double totalWeight = 0;
        double earnedWeight = 0;
        boolean allRequiredMet = true;

        for (MatchSearchRequest.SkillCriterion criterion : criteria) {
            String skillKey = criterion.getSkillName().toLowerCase();
            double weight = criterion.getWeight() != null ? criterion.getWeight() : 1.0;
            totalWeight += weight;

            CandidateSkill match = candidateSkillMap.get(skillKey);
            String typeLabel = criterion.isRequired() ? "required" : "preferred";

            if (match != null) {
                boolean yearsMet = true;
                if (criterion.getMinYears() != null && criterion.getMinYears() > 0) {
                    if (match.getYearsExperience() != null && match.getYearsExperience() >= criterion.getMinYears()) {
                        result.getMatchedRationale().add(
                                criterion.getSkillName() + " " + criterion.getMinYears() + "+ years " +
                                typeLabel + " met (" + match.getYearsExperience() + " years, " +
                                match.getProficiency() + ")");
                    } else {
                        yearsMet = false;
                        int actual = match.getYearsExperience() != null ? match.getYearsExperience() : 0;
                        result.getUnmatchedRationale().add(
                                criterion.getSkillName() + " " + criterion.getMinYears() + "+ years " +
                                typeLabel + " not met (has " + actual + " years)");
                    }
                } else {
                    result.getMatchedRationale().add(
                            criterion.getSkillName() + " " + typeLabel + " met" +
                            (match.getProficiency() != null ? " (" + match.getProficiency() + ")" : ""));
                }

                if (yearsMet) {
                    earnedWeight += weight;
                } else {
                    double partialCredit = weight * 0.5;
                    earnedWeight += partialCredit;
                    if (criterion.isRequired()) {
                        allRequiredMet = false;
                    }
                }
            } else {
                result.getUnmatchedRationale().add(
                        criterion.getSkillName() +
                        (criterion.getMinYears() != null ? " " + criterion.getMinYears() + "+ years" : "") +
                        " " + typeLabel + " not met (skill not found)");
                if (criterion.isRequired()) {
                    allRequiredMet = false;
                }
            }
        }

        double score = totalWeight > 0 ? earnedWeight / totalWeight : 0;

        if (request.getOperator() == BooleanOperator.AND && !allRequiredMet) {
            score *= 0.5;
        }

        result.setMatchScore(score);
        result.setMeetsAllRequired(allRequiredMet);
        return result;
    }
}
