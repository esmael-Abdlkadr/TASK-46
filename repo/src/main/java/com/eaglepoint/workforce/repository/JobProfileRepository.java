package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.JobProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobProfileRepository extends JpaRepository<JobProfile, Long> {

    List<JobProfile> findByActiveTrue();

    @Query("SELECT jp FROM JobProfile jp LEFT JOIN FETCH jp.criteria WHERE jp.id = :id")
    Optional<JobProfile> findByIdWithCriteria(@Param("id") Long id);
}
