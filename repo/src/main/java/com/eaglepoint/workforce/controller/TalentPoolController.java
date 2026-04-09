package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.TalentPool;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.CandidateService;
import com.eaglepoint.workforce.service.TalentPoolService;
import com.eaglepoint.workforce.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter/talent-pools")
@PreAuthorize("hasRole('RECRUITER') or hasRole('ADMINISTRATOR')")
public class TalentPoolController {

    private final TalentPoolService talentPoolService;
    private final CandidateService candidateService;
    private final UserService userService;

    public TalentPoolController(TalentPoolService talentPoolService,
                                 CandidateService candidateService,
                                 UserService userService) {
        this.talentPoolService = talentPoolService;
        this.candidateService = candidateService;
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "TalentPoolList")
    public String list(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        model.addAttribute("pools", talentPoolService.findByCreator(userId));
        return "recruiter/talent-pools";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "TalentPool")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttrs) {
        Long userId = getUserId(userDetails);
        talentPoolService.create(name, description, userId);
        redirectAttrs.addFlashAttribute("success", "Talent pool '" + name + "' created");
        return "redirect:/recruiter/talent-pools";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "TalentPoolDetail")
    public String view(@PathVariable Long id, Model model) {
        TalentPool pool = talentPoolService.findByIdWithCandidates(id)
                .orElseThrow(() -> new RuntimeException("Talent pool not found"));
        model.addAttribute("pool", pool);
        model.addAttribute("allCandidates", candidateService.findAll());
        return "recruiter/talent-pool-detail";
    }

    @PostMapping("/{id}/add-candidates")
    @Audited(action = AuditAction.UPDATE, resource = "TalentPool")
    public String addCandidates(@PathVariable Long id,
                                @RequestParam List<Long> candidateIds,
                                RedirectAttributes redirectAttrs) {
        talentPoolService.addCandidates(id, candidateIds);
        redirectAttrs.addFlashAttribute("success", "Added " + candidateIds.size() + " candidates to pool");
        return "redirect:/recruiter/talent-pools/" + id;
    }

    @PostMapping("/{id}/remove-candidates")
    @Audited(action = AuditAction.UPDATE, resource = "TalentPool")
    public String removeCandidates(@PathVariable Long id,
                                   @RequestParam List<Long> candidateIds,
                                   RedirectAttributes redirectAttrs) {
        talentPoolService.removeCandidates(id, candidateIds);
        redirectAttrs.addFlashAttribute("success", "Removed " + candidateIds.size() + " candidates from pool");
        return "redirect:/recruiter/talent-pools/" + id;
    }

    @PostMapping("/{id}/delete")
    @Audited(action = AuditAction.DELETE, resource = "TalentPool")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        talentPoolService.delete(id);
        redirectAttrs.addFlashAttribute("success", "Talent pool deleted");
        return "redirect:/recruiter/talent-pools";
    }

    private Long getUserId(UserDetails userDetails) {
        return userService.findByUsername(userDetails.getUsername())
                .map(User::getId).orElse(0L);
    }
}
