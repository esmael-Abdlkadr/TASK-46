package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.entity.CandidateSkill;
import com.eaglepoint.workforce.enums.PipelineStage;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
import com.eaglepoint.workforce.repository.CandidateSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    private final CandidateProfileRepository candidateRepository;
    private final CandidateSkillRepository skillRepository;

    public CandidateService(CandidateProfileRepository candidateRepository,
                            CandidateSkillRepository skillRepository) {
        this.candidateRepository = candidateRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<CandidateProfile> findAll() {
        return candidateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CandidateProfile> findById(Long id) {
        return candidateRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CandidateProfile> findByStage(PipelineStage stage) {
        return candidateRepository.findByPipelineStage(stage);
    }

    @Transactional
    public CandidateProfile save(CandidateProfile candidate) {
        return candidateRepository.save(candidate);
    }

    @Transactional
    public void bulkTag(List<Long> candidateIds, String tag) {
        List<CandidateProfile> candidates = candidateRepository.findAllById(candidateIds);
        for (CandidateProfile candidate : candidates) {
            String existing = candidate.getTags();
            if (existing == null || existing.isBlank()) {
                candidate.setTags(tag);
            } else {
                Set<String> tags = new LinkedHashSet<>(Arrays.asList(existing.split(",")));
                tags.add(tag.trim());
                candidate.setTags(String.join(",", tags));
            }
        }
        candidateRepository.saveAll(candidates);
    }

    @Transactional(readOnly = true)
    public List<String> getAllDistinctSkillNames() {
        return skillRepository.findDistinctSkillNames();
    }

    @Transactional(readOnly = true)
    public Map<PipelineStage, Long> getPipelineStageCounts() {
        Map<PipelineStage, Long> counts = new LinkedHashMap<>();
        for (PipelineStage stage : PipelineStage.values()) {
            counts.put(stage, candidateRepository.countByPipelineStage(stage));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return candidateRepository.findAll().stream()
                .map(CandidateProfile::getTags)
                .filter(t -> t != null && !t.isBlank())
                .flatMap(t -> Arrays.stream(t.split(",")))
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
