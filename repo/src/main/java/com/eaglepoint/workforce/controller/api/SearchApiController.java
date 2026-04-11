package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.dto.UnifiedSearchRequest;
import com.eaglepoint.workforce.dto.UnifiedSearchResult;
import com.eaglepoint.workforce.entity.SavedSearch;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.SavedSearchService;
import com.eaglepoint.workforce.service.ResourceAuthorizationService;
import com.eaglepoint.workforce.service.UnifiedSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchApiController {

    private final UnifiedSearchService searchService;
    private final SavedSearchService savedSearchService;
    private final ResourceAuthorizationService authzService;

    public SearchApiController(UnifiedSearchService searchService,
                               SavedSearchService savedSearchService,
                               ResourceAuthorizationService authzService) {
        this.searchService = searchService;
        this.savedSearchService = savedSearchService;
        this.authzService = authzService;
    }

    @PostMapping
    @Audited(action = AuditAction.READ, resource = "API:UnifiedSearch")
    public ResponseEntity<List<UnifiedSearchResult>> search(@Valid @RequestBody UnifiedSearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }

    @GetMapping("/saved")
    @Audited(action = AuditAction.READ, resource = "API:SavedSearchList")
    public ResponseEntity<List<SavedSearch>> savedSearches(Authentication auth) {
        Long userId = authzService.resolveUserId(auth);
        return ResponseEntity.ok(authzService.isAdmin(auth)
                ? savedSearchService.findAll() : savedSearchService.findByCreator(userId));
    }

    @GetMapping("/saved/{id}")
    @Audited(action = AuditAction.READ, resource = "API:SavedSearchDetail")
    public ResponseEntity<SavedSearch> savedSearchDetail(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(authzService.authorizeSavedSearch(id, auth));
    }
}
