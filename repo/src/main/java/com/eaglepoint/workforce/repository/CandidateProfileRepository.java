package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.enums.PipelineStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long>,
        JpaSpecificationExecutor<CandidateProfile> {

    List<CandidateProfile> findByPipelineStage(PipelineStage stage);

    long countByPipelineStage(PipelineStage stage);

    @Query("SELECT c FROM CandidateProfile c LEFT JOIN FETCH c.skills WHERE c.id IN :ids")
    List<CandidateProfile> findByIdInWithSkills(@Param("ids") List<Long> ids);

    @Query("SELECT c FROM CandidateProfile c LEFT JOIN FETCH c.skills")
    List<CandidateProfile> findAllWithSkills();

    @Query("SELECT c FROM CandidateProfile c LEFT JOIN FETCH c.skills " +
           "WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.summary) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.currentTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<CandidateProfile> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT c FROM CandidateProfile c LEFT JOIN FETCH c.skills s " +
           "WHERE s.skillName IN :skillNames")
    List<CandidateProfile> findBySkillNames(@Param("skillNames") List<String> skillNames);
}
