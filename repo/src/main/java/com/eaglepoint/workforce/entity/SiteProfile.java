package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.DispatchMode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "site_profiles", indexes = {
    @Index(name = "idx_site_zone", columnList = "zone"),
    @Index(name = "idx_site_active", columnList = "active")
})
public class SiteProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String zone;

    @Column(name = "capacity_limit", nullable = false)
    private Integer capacityLimit = 10;

    @Column(name = "current_occupancy", nullable = false)
    private Integer currentOccupancy = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_mode", nullable = false, length = 20)
    private DispatchMode dispatchMode = DispatchMode.ASSIGNED_ORDER;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

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

    public boolean isAtCapacity() {
        return currentOccupancy >= capacityLimit;
    }

    public int getRemainingCapacity() {
        return Math.max(0, capacityLimit - currentOccupancy);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public Integer getCapacityLimit() { return capacityLimit; }
    public void setCapacityLimit(Integer capacityLimit) { this.capacityLimit = capacityLimit; }
    public Integer getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(Integer currentOccupancy) { this.currentOccupancy = currentOccupancy; }
    public DispatchMode getDispatchMode() { return dispatchMode; }
    public void setDispatchMode(DispatchMode dispatchMode) { this.dispatchMode = dispatchMode; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
