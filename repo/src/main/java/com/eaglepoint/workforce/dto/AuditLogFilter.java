package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.enums.AuditAction;
import java.time.LocalDateTime;

public class AuditLogFilter {

    private Long userId;
    private AuditAction action;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private String workstationId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    public LocalDateTime getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDateTime dateFrom) { this.dateFrom = dateFrom; }
    public LocalDateTime getDateTo() { return dateTo; }
    public void setDateTo(LocalDateTime dateTo) { this.dateTo = dateTo; }
    public String getWorkstationId() { return workstationId; }
    public void setWorkstationId(String workstationId) { this.workstationId = workstationId; }
}
