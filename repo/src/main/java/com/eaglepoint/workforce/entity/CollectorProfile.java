package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.CollectorStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "collector_profiles", indexes = {
    @Index(name = "idx_collector_status", columnList = "status"),
    @Index(name = "idx_collector_zone", columnList = "zone")
})
public class CollectorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "employee_id", unique = true, length = 30)
    private String employeeId;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 100)
    private String zone;

    @Column(length = 500)
    private String skills;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollectorStatus status = CollectorStatus.AVAILABLE;

    @Column(name = "max_concurrent_jobs")
    private Integer maxConcurrentJobs = 1;

    @OneToMany(mappedBy = "collector", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkShift> workShifts = new ArrayList<>();

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

    public String getFullName() { return firstName + " " + lastName; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public CollectorStatus getStatus() { return status; }
    public void setStatus(CollectorStatus status) { this.status = status; }
    public Integer getMaxConcurrentJobs() { return maxConcurrentJobs; }
    public void setMaxConcurrentJobs(Integer maxConcurrentJobs) { this.maxConcurrentJobs = maxConcurrentJobs; }
    public List<WorkShift> getWorkShifts() { return workShifts; }
    public void setWorkShifts(List<WorkShift> workShifts) { this.workShifts = workShifts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
