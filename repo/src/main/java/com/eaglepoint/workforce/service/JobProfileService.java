package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.JobProfile;
import com.eaglepoint.workforce.entity.JobProfileCriterion;
import com.eaglepoint.workforce.repository.JobProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class JobProfileService {

    private final JobProfileRepository jobProfileRepository;

    public JobProfileService(JobProfileRepository jobProfileRepository) {
        this.jobProfileRepository = jobProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<JobProfile> findAllActive() {
        return jobProfileRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<JobProfile> findAll() {
        return jobProfileRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<JobProfile> findByIdWithCriteria(Long id) {
        return jobProfileRepository.findByIdWithCriteria(id);
    }

    @Transactional
    public JobProfile save(JobProfile jobProfile) {
        for (JobProfileCriterion criterion : jobProfile.getCriteria()) {
            criterion.setJobProfile(jobProfile);
        }
        return jobProfileRepository.save(jobProfile);
    }
}
