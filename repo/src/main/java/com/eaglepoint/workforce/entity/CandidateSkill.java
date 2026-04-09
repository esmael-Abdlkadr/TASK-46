package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.SkillProficiency;
import jakarta.persistence.*;

@Entity
@Table(name = "candidate_skills", indexes = {
    @Index(name = "idx_cskill_name", columnList = "skill_name"),
    @Index(name = "idx_cskill_candidate", columnList = "candidate_id")
})
public class CandidateSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SkillProficiency proficiency;

    public CandidateSkill() {}

    public CandidateSkill(CandidateProfile candidateProfile, String skillName,
                          Integer yearsExperience, SkillProficiency proficiency) {
        this.candidateProfile = candidateProfile;
        this.skillName = skillName;
        this.yearsExperience = yearsExperience;
        this.proficiency = proficiency;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CandidateProfile getCandidateProfile() { return candidateProfile; }
    public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }
    public SkillProficiency getProficiency() { return proficiency; }
    public void setProficiency(SkillProficiency proficiency) { this.proficiency = proficiency; }
}
