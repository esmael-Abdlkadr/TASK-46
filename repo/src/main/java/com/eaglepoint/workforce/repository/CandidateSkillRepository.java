package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.CandidateSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, Long> {

    List<CandidateSkill> findByCandidateProfileId(Long candidateId);

    @Query("SELECT DISTINCT s.skillName FROM CandidateSkill s ORDER BY s.skillName")
    List<String> findDistinctSkillNames();
}
