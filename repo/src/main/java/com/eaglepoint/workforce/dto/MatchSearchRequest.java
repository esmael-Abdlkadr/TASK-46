package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.enums.BooleanOperator;
import java.util.ArrayList;
import java.util.List;

public class MatchSearchRequest {

    private Long jobProfileId;
    private List<SkillCriterion> skillCriteria = new ArrayList<>();
    private BooleanOperator operator = BooleanOperator.AND;
    private String locationFilter;
    private Integer minYearsExperience;
    private Integer maxYearsExperience;
    private String educationLevel;
    private String keyword;

    public Long getJobProfileId() { return jobProfileId; }
    public void setJobProfileId(Long jobProfileId) { this.jobProfileId = jobProfileId; }
    public List<SkillCriterion> getSkillCriteria() { return skillCriteria; }
    public void setSkillCriteria(List<SkillCriterion> skillCriteria) { this.skillCriteria = skillCriteria; }
    public BooleanOperator getOperator() { return operator; }
    public void setOperator(BooleanOperator operator) { this.operator = operator; }
    public String getLocationFilter() { return locationFilter; }
    public void setLocationFilter(String locationFilter) { this.locationFilter = locationFilter; }
    public Integer getMinYearsExperience() { return minYearsExperience; }
    public void setMinYearsExperience(Integer minYearsExperience) { this.minYearsExperience = minYearsExperience; }
    public Integer getMaxYearsExperience() { return maxYearsExperience; }
    public void setMaxYearsExperience(Integer maxYearsExperience) { this.maxYearsExperience = maxYearsExperience; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public static class SkillCriterion {
        private String skillName;
        private Integer minYears;
        private Double weight = 1.0;
        private boolean required = true;

        public String getSkillName() { return skillName; }
        public void setSkillName(String skillName) { this.skillName = skillName; }
        public Integer getMinYears() { return minYears; }
        public void setMinYears(Integer minYears) { this.minYears = minYears; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
    }
}
