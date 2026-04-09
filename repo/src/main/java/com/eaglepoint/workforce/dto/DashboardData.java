package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.entity.ExceptionAlert;
import com.eaglepoint.workforce.entity.Job;
import com.eaglepoint.workforce.entity.Requisition;
import java.util.List;

public class DashboardData {

    private long openRequisitions;
    private long unassignedJobs;
    private long activeAlerts;
    private List<Requisition> recentRequisitions;
    private List<Job> recentUnassignedJobs;
    private List<ExceptionAlert> recentAlerts;

    public long getOpenRequisitions() { return openRequisitions; }
    public void setOpenRequisitions(long openRequisitions) { this.openRequisitions = openRequisitions; }
    public long getUnassignedJobs() { return unassignedJobs; }
    public void setUnassignedJobs(long unassignedJobs) { this.unassignedJobs = unassignedJobs; }
    public long getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(long activeAlerts) { this.activeAlerts = activeAlerts; }
    public List<Requisition> getRecentRequisitions() { return recentRequisitions; }
    public void setRecentRequisitions(List<Requisition> recentRequisitions) { this.recentRequisitions = recentRequisitions; }
    public List<Job> getRecentUnassignedJobs() { return recentUnassignedJobs; }
    public void setRecentUnassignedJobs(List<Job> recentUnassignedJobs) { this.recentUnassignedJobs = recentUnassignedJobs; }
    public List<ExceptionAlert> getRecentAlerts() { return recentAlerts; }
    public void setRecentAlerts(List<ExceptionAlert> recentAlerts) { this.recentAlerts = recentAlerts; }
}
