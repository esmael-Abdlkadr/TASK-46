package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.entity.TalentPool;
import com.eaglepoint.workforce.exception.ResourceNotFoundException;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
import com.eaglepoint.workforce.repository.TalentPoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TalentPoolService {

    private final TalentPoolRepository poolRepository;
    private final CandidateProfileRepository candidateRepository;

    public TalentPoolService(TalentPoolRepository poolRepository,
                             CandidateProfileRepository candidateRepository) {
        this.poolRepository = poolRepository;
        this.candidateRepository = candidateRepository;
    }

    @Transactional(readOnly = true)
    public List<TalentPool> findAll() {
        return poolRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TalentPool> findByCreator(Long userId) {
        return poolRepository.findByCreatedBy(userId);
    }

    @Transactional(readOnly = true)
    public Optional<TalentPool> findByIdWithCandidates(Long id) {
        return poolRepository.findByIdWithCandidates(id);
    }

    @Transactional
    public TalentPool create(String name, String description, Long createdBy) {
        TalentPool pool = new TalentPool();
        pool.setName(name);
        pool.setDescription(description);
        pool.setCreatedBy(createdBy);
        return poolRepository.save(pool);
    }

    @Transactional
    public TalentPool addCandidates(Long poolId, List<Long> candidateIds) {
        TalentPool pool = poolRepository.findByIdWithCandidates(poolId)
                .orElseThrow(() -> new ResourceNotFoundException("Talent pool not found: " + poolId));
        List<CandidateProfile> candidates = candidateRepository.findAllById(candidateIds);
        pool.getCandidates().addAll(candidates);
        return poolRepository.save(pool);
    }

    @Transactional
    public TalentPool removeCandidates(Long poolId, List<Long> candidateIds) {
        TalentPool pool = poolRepository.findByIdWithCandidates(poolId)
                .orElseThrow(() -> new ResourceNotFoundException("Talent pool not found: " + poolId));
        Set<CandidateProfile> toRemove = new java.util.HashSet<>(candidateRepository.findAllById(candidateIds));
        pool.getCandidates().removeAll(toRemove);
        return poolRepository.save(pool);
    }

    @Transactional
    public void delete(Long poolId) {
        poolRepository.deleteById(poolId);
    }
}
