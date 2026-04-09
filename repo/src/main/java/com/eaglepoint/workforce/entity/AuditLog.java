package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.AuditAction;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_workstation", columnList = "workstation_id")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(length = 500)
    private String detail;

    @Column(name = "workstation_id", length = 100)
    private String workstationId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    protected AuditLog() {}

    public AuditLog(Long userId, String username, AuditAction action, String resource,
                    Long resourceId, String detail, String workstationId) {
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.resource = resource;
        this.resourceId = resourceId;
        this.detail = detail;
        this.workstationId = workstationId;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public AuditAction getAction() { return action; }
    public String getResource() { return resource; }
    public Long getResourceId() { return resourceId; }
    public String getDetail() { return detail; }
    public String getWorkstationId() { return workstationId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
