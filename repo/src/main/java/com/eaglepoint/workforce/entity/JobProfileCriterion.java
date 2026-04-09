package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.CriterionType;
import jakarta.persistence.*;

@Entity
@Table(name = "job_profile_criteria")
public class JobProfileCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_profile_id", nullable = false)
    private JobProfile jobProfile;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "min_years")
    private Integer minYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "criterion_type", nullable = false, length = 20)
    private CriterionType criterionType = CriterionType.REQUIRED;

    @Column(nullable = false)
    private Double weight = 1.0;

    public JobProfileCriterion() {}

    public JobProfileCriterion(JobProfile jobProfile, String skillName, Integer minYears,
                               CriterionType criterionType, Double weight) {
        this.jobProfile = jobProfile;
        this.skillName = skillName;
        this.minYears = minYears;
        this.criterionType = criterionType;
        this.weight = weight;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public JobProfile getJobProfile() { return jobProfile; }
    public void setJobProfile(JobProfile jobProfile) { this.jobProfile = jobProfile; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Integer getMinYears() { return minYears; }
    public void setMinYears(Integer minYears) { this.minYears = minYears; }
    public CriterionType getCriterionType() { return criterionType; }
    public void setCriterionType(CriterionType criterionType) { this.criterionType = criterionType; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}
