package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.dto.MatchResult;
import com.eaglepoint.workforce.dto.MatchSearchRequest;
import com.eaglepoint.workforce.entity.SavedSearch;
import com.eaglepoint.workforce.entity.SearchSnapshot;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.BooleanOperator;
import com.eaglepoint.workforce.enums.CriterionType;
import com.eaglepoint.workforce.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/recruiter/search")
@PreAuthorize("hasRole('RECRUITER') or hasRole('ADMINISTRATOR')")
public class MatchSearchController {

    private final MatchingService matchingService;
    private final SavedSearchService savedSearchService;
    private final SearchSnapshotService snapshotService;
    private final JobProfileService jobProfileService;
    private final CandidateService candidateService;
    private final UserService userService;
    private final ResourceAuthorizationService authzService;

    public MatchSearchController(MatchingService matchingService,
                                  SavedSearchService savedSearchService,
                                  SearchSnapshotService snapshotService,
                                  JobProfileService jobProfileService,
                                  CandidateService candidateService,
                                  UserService userService,
                                  ResourceAuthorizationService authzService) {
        this.matchingService = matchingService;
        this.savedSearchService = savedSearchService;
        this.snapshotService = snapshotService;
        this.jobProfileService = jobProfileService;
        this.candidateService = candidateService;
        this.userService = userService;
        this.authzService = authzService;
    }

    @GetMapping
    public String searchForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("searchRequest", new MatchSearchRequest());
        model.addAttribute("operators", BooleanOperator.values());
        model.addAttribute("jobProfiles", jobProfileService.findAllActive());
        model.addAttribute("allSkills", candidateService.getAllDistinctSkillNames());

        Long userId = getUserId(userDetails);
        model.addAttribute("savedSearches", savedSearchService.findByCreator(userId));
        model.addAttribute("snapshots", snapshotService.findByCreator(userId));
        return "recruiter/search";
    }

    @PostMapping("/execute")
    @Audited(action = AuditAction.READ, resource = "MatchSearch")
    public String executeSearch(@RequestParam(required = false) Long jobProfileId,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String locationFilter,
                                @RequestParam(required = false) Integer minYearsExperience,
                                @RequestParam(required = false) Integer maxYearsExperience,
                                @RequestParam(required = false) String educationLevel,
                                @RequestParam(required = false) BooleanOperator operator,
                                @RequestParam(required = false) List<String> skillNames,
                                @RequestParam(required = false) List<Integer> skillMinYears,
                                @RequestParam(required = false) List<Double> skillWeights,
                                @RequestParam(required = false) List<Boolean> skillRequired,
                                Model model,
                                @AuthenticationPrincipal UserDetails userDetails) {
        MatchSearchRequest request = buildRequest(jobProfileId, keyword, locationFilter,
                minYearsExperience, maxYearsExperience, educationLevel, operator,
                skillNames, skillMinYears, skillWeights, skillRequired);

        List<MatchResult> results;
        if (jobProfileId != null) {
            results = matchingService.matchAgainstJobProfile(jobProfileId);
        } else {
            results = matchingService.matchCandidates(request);
        }

        model.addAttribute("results", results);
        model.addAttribute("searchRequest", request);
        model.addAttribute("operators", BooleanOperator.values());
        model.addAttribute("jobProfiles", jobProfileService.findAllActive());
        model.addAttribute("allSkills", candidateService.getAllDistinctSkillNames());

        Long userId = getUserId(userDetails);
        model.addAttribute("savedSearches", savedSearchService.findByCreator(userId));
        model.addAttribute("snapshots", snapshotService.findByCreator(userId));
        return "recruiter/search";
    }

    @PostMapping("/save")
    @Audited(action = AuditAction.CREATE, resource = "SavedSearch")
    public String saveSearch(@RequestParam String searchName,
                             @RequestParam(required = false) Long jobProfileId,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String locationFilter,
                             @RequestParam(required = false) Integer minYearsExperience,
                             @RequestParam(required = false) Integer maxYearsExperience,
                             @RequestParam(required = false) String educationLevel,
                             @RequestParam(required = false) BooleanOperator operator,
                             @RequestParam(required = false) List<String> skillNames,
                             @RequestParam(required = false) List<Integer> skillMinYears,
                             @RequestParam(required = false) List<Double> skillWeights,
                             @RequestParam(required = false) List<Boolean> skillRequired,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttrs) {
        MatchSearchRequest request = buildRequest(jobProfileId, keyword, locationFilter,
                minYearsExperience, maxYearsExperience, educationLevel, operator,
                skillNames, skillMinYears, skillWeights, skillRequired);

        Long userId = getUserId(userDetails);
        savedSearchService.save(searchName, request, userId);
        redirectAttrs.addFlashAttribute("success", "Search saved as '" + searchName + "'");
        return "redirect:/recruiter/search";
    }

    @GetMapping("/saved/{id}/load")
    public String loadSavedSearch(@PathVariable Long id, Model model,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Authentication auth) {
        SavedSearch ss = authzService.authorizeSavedSearch(id, auth);
        MatchSearchRequest request = savedSearchService.deserializeCriteria(ss.getSearchCriteriaJson());

        model.addAttribute("searchRequest", request);
        model.addAttribute("operators", BooleanOperator.values());
        model.addAttribute("jobProfiles", jobProfileService.findAllActive());
        model.addAttribute("allSkills", candidateService.getAllDistinctSkillNames());

        Long userId = getUserId(userDetails);
        model.addAttribute("savedSearches", savedSearchService.findByCreator(userId));
        model.addAttribute("snapshots", snapshotService.findByCreator(userId));
        model.addAttribute("loadedSearchName", ss.getName());
        return "recruiter/search";
    }

    @PostMapping("/snapshot")
    @Audited(action = AuditAction.CREATE, resource = "SearchSnapshot")
    public String createSnapshot(@RequestParam String snapshotName,
                                  @RequestParam(required = false) Long jobProfileId,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String locationFilter,
                                  @RequestParam(required = false) Integer minYearsExperience,
                                  @RequestParam(required = false) Integer maxYearsExperience,
                                  @RequestParam(required = false) String educationLevel,
                                  @RequestParam(required = false) BooleanOperator operator,
                                  @RequestParam(required = false) List<String> skillNames,
                                  @RequestParam(required = false) List<Integer> skillMinYears,
                                  @RequestParam(required = false) List<Double> skillWeights,
                                  @RequestParam(required = false) List<Boolean> skillRequired,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttrs) {
        MatchSearchRequest request = buildRequest(jobProfileId, keyword, locationFilter,
                minYearsExperience, maxYearsExperience, educationLevel, operator,
                skillNames, skillMinYears, skillWeights, skillRequired);

        Long userId = getUserId(userDetails);

        if (jobProfileId != null) {
            snapshotService.createSnapshotFromJobProfile(snapshotName, jobProfileId, userId);
        } else {
            snapshotService.createSnapshot(snapshotName, request, userId);
        }

        redirectAttrs.addFlashAttribute("success", "Snapshot '" + snapshotName + "' created");
        return "redirect:/recruiter/search";
    }

    @GetMapping("/snapshot/{id}")
    @Audited(action = AuditAction.READ, resource = "SearchSnapshot")
    public String viewSnapshot(@PathVariable Long id, Model model, Authentication auth) {
        SearchSnapshot snapshot = authzService.authorizeSnapshot(id, auth);
        model.addAttribute("snapshot", snapshot);
        return "recruiter/snapshot-detail";
    }

    private MatchSearchRequest buildRequest(Long jobProfileId, String keyword, String locationFilter,
                                             Integer minYearsExperience, Integer maxYearsExperience,
                                             String educationLevel, BooleanOperator operator,
                                             List<String> skillNames, List<Integer> skillMinYears,
                                             List<Double> skillWeights, List<Boolean> skillRequired) {
        MatchSearchRequest request = new MatchSearchRequest();
        request.setJobProfileId(jobProfileId);
        request.setKeyword(keyword);
        request.setLocationFilter(locationFilter);
        request.setMinYearsExperience(minYearsExperience);
        request.setMaxYearsExperience(maxYearsExperience);
        request.setEducationLevel(educationLevel);
        request.setOperator(operator != null ? operator : BooleanOperator.AND);

        if (skillNames != null) {
            List<MatchSearchRequest.SkillCriterion> criteria = new ArrayList<>();
            for (int i = 0; i < skillNames.size(); i++) {
                String name = skillNames.get(i);
                if (name == null || name.isBlank()) continue;
                MatchSearchRequest.SkillCriterion sc = new MatchSearchRequest.SkillCriterion();
                sc.setSkillName(name.trim());
                sc.setMinYears((skillMinYears != null && i < skillMinYears.size()) ? skillMinYears.get(i) : null);
                sc.setWeight((skillWeights != null && i < skillWeights.size()) ? skillWeights.get(i) : 1.0);
                sc.setRequired(skillRequired == null || i >= skillRequired.size() || skillRequired.get(i));
                criteria.add(sc);
            }
            request.setSkillCriteria(criteria);
        }

        return request;
    }

    private Long getUserId(UserDetails userDetails) {
        return userService.findByUsername(userDetails.getUsername())
                .map(User::getId)
                .orElse(0L);
    }
}
