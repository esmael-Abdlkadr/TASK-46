package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.PipelineStage;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidate_profiles", indexes = {
    @Index(name = "idx_candidate_name", columnList = "first_name, last_name"),
    @Index(name = "idx_candidate_stage", columnList = "pipeline_stage"),
    @Index(name = "idx_candidate_location", columnList = "location")
})
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 200)
    private String location;

    @Column(name = "current_title", length = 200)
    private String currentTitle;

    @Column(name = "current_employer", length = 200)
    private String currentEmployer;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Column(length = 2000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_stage", nullable = false, length = 30)
    private PipelineStage pipelineStage = PipelineStage.SOURCED;

    @Column(length = 500)
    private String tags;

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateSkill> skills = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCurrentTitle() { return currentTitle; }
    public void setCurrentTitle(String currentTitle) { this.currentTitle = currentTitle; }
    public String getCurrentEmployer() { return currentEmployer; }
    public void setCurrentEmployer(String currentEmployer) { this.currentEmployer = currentEmployer; }
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public PipelineStage getPipelineStage() { return pipelineStage; }
    public void setPipelineStage(PipelineStage pipelineStage) { this.pipelineStage = pipelineStage; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public List<CandidateSkill> getSkills() { return skills; }
    public void setSkills(List<CandidateSkill> skills) { this.skills = skills; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
