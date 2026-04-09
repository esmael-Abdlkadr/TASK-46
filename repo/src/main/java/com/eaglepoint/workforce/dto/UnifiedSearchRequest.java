package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.enums.SearchDomain;
import java.util.Set;

public class UnifiedSearchRequest {

    private String keyword;
    private Set<SearchDomain> domains;
    private String locationFilter;
    private String departmentFilter;
    private String statusFilter;
    private String dateFrom;
    private String dateTo;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Set<SearchDomain> getDomains() { return domains; }
    public void setDomains(Set<SearchDomain> domains) { this.domains = domains; }
    public String getLocationFilter() { return locationFilter; }
    public void setLocationFilter(String locationFilter) { this.locationFilter = locationFilter; }
    public String getDepartmentFilter() { return departmentFilter; }
    public void setDepartmentFilter(String departmentFilter) { this.departmentFilter = departmentFilter; }
    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }
    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
}
