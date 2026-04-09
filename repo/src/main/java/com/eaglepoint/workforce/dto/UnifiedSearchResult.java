package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.enums.SearchDomain;

public class UnifiedSearchResult {

    private SearchDomain domain;
    private Long entityId;
    private String title;
    private String subtitle;
    private String status;
    private String detailUrl;

    public UnifiedSearchResult(SearchDomain domain, Long entityId, String title,
                                String subtitle, String status, String detailUrl) {
        this.domain = domain;
        this.entityId = entityId;
        this.title = title;
        this.subtitle = subtitle;
        this.status = status;
        this.detailUrl = detailUrl;
    }

    public SearchDomain getDomain() { return domain; }
    public Long getEntityId() { return entityId; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getStatus() { return status; }
    public String getDetailUrl() { return detailUrl; }
}
