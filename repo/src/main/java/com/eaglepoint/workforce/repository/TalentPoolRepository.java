package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.TalentPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TalentPoolRepository extends JpaRepository<TalentPool, Long> {

    List<TalentPool> findByCreatedBy(Long createdBy);

    @Query("SELECT tp FROM TalentPool tp LEFT JOIN FETCH tp.candidates WHERE tp.id = :id")
    Optional<TalentPool> findByIdWithCandidates(@Param("id") Long id);

    @Query("SELECT tp FROM TalentPool tp LEFT JOIN FETCH tp.candidates WHERE tp.id = :id AND tp.createdBy = :userId")
    Optional<TalentPool> findByIdAndCreatedByWithCandidates(@Param("id") Long id, @Param("userId") Long userId);

    Optional<TalentPool> findByIdAndCreatedBy(Long id, Long createdBy);
}
