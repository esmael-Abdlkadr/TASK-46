package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.dto.UnifiedSearchRequest;
import com.eaglepoint.workforce.dto.UnifiedSearchResult;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.UnifiedSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchApiController {

    private final UnifiedSearchService searchService;

    public SearchApiController(UnifiedSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping
    @Audited(action = AuditAction.READ, resource = "API:UnifiedSearch")
    public ResponseEntity<List<UnifiedSearchResult>> search(@RequestBody UnifiedSearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }
}
