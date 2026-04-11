package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.dto.UnifiedSearchRequest;
import com.eaglepoint.workforce.dto.UnifiedSearchResult;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.SearchDomain;
import com.eaglepoint.workforce.service.UnifiedSearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/search")
public class UnifiedSearchController {

    private final UnifiedSearchService searchService;

    public UnifiedSearchController(UnifiedSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public String searchForm(Model model) {
        model.addAttribute("searchRequest", new UnifiedSearchRequest());
        model.addAttribute("domains", SearchDomain.values());
        return "search/unified";
    }

    @GetMapping("/unified")
    public String searchFormAlias(Model model) {
        return searchForm(model);
    }

    @PostMapping
    @Audited(action = AuditAction.READ, resource = "UnifiedSearch")
    public String executeSearch(@ModelAttribute UnifiedSearchRequest request, Model model) {
        List<UnifiedSearchResult> results = searchService.search(request);
        model.addAttribute("results", results);
        model.addAttribute("searchRequest", request);
        model.addAttribute("domains", SearchDomain.values());
        model.addAttribute("resultCount", results.size());
        return "search/unified";
    }

    @PostMapping("/unified")
    @Audited(action = AuditAction.READ, resource = "UnifiedSearch")
    public String executeSearchAlias(@ModelAttribute UnifiedSearchRequest request, Model model) {
        return executeSearch(request, model);
    }
}
