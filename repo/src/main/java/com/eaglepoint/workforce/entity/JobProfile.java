package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_profiles")
public class JobProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String department;

    @Column(length = 200)
    private String location;

    @Column(length = 2000)
    private String description;

    @Column(name = "min_years_experience")
    private Integer minYearsExperience;

    @Column(name = "education_requirement", length = 100)
    private String educationRequirement;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "jobProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobProfileCriterion> criteria = new ArrayList<>();

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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getMinYearsExperience() { return minYearsExperience; }
    public void setMinYearsExperience(Integer minYearsExperience) { this.minYearsExperience = minYearsExperience; }
    public String getEducationRequirement() { return educationRequirement; }
    public void setEducationRequirement(String educationRequirement) { this.educationRequirement = educationRequirement; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<JobProfileCriterion> getCriteria() { return criteria; }
    public void setCriteria(List<JobProfileCriterion> criteria) { this.criteria = criteria; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
