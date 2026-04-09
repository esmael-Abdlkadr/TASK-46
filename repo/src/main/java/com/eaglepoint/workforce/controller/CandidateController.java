package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.entity.CandidateSkill;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.PipelineStage;
import com.eaglepoint.workforce.enums.SkillProficiency;
import com.eaglepoint.workforce.service.CandidateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/recruiter/candidates")
@PreAuthorize("hasRole('RECRUITER') or hasRole('ADMINISTRATOR')")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "CandidateList")
    public String list(@RequestParam(required = false) PipelineStage stage, Model model) {
        List<CandidateProfile> candidates;
        if (stage != null) {
            candidates = candidateService.findByStage(stage);
        } else {
            candidates = candidateService.findAll();
        }
        model.addAttribute("candidates", candidates);
        model.addAttribute("stages", PipelineStage.values());
        model.addAttribute("selectedStage", stage);
        model.addAttribute("stageCounts", candidateService.getPipelineStageCounts());
        return "recruiter/candidates";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("candidate", new CandidateProfile());
        model.addAttribute("stages", PipelineStage.values());
        model.addAttribute("proficiencies", SkillProficiency.values());
        return "recruiter/candidate-form";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "Candidate")
    public String create(@ModelAttribute CandidateProfile candidate,
                         @RequestParam(required = false) List<String> skillNames,
                         @RequestParam(required = false) List<Integer> skillYears,
                         @RequestParam(required = false) List<SkillProficiency> skillProficiencies,
                         RedirectAttributes redirectAttrs) {
        addSkillsToCandidate(candidate, skillNames, skillYears, skillProficiencies);
        candidateService.save(candidate);
        redirectAttrs.addFlashAttribute("success", "Candidate created successfully");
        return "redirect:/recruiter/candidates";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "CandidateDetail")
    public String view(@PathVariable Long id, Model model) {
        CandidateProfile candidate = candidateService.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
        model.addAttribute("candidate", candidate);
        return "recruiter/candidate-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CandidateProfile candidate = candidateService.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
        model.addAttribute("candidate", candidate);
        model.addAttribute("stages", PipelineStage.values());
        model.addAttribute("proficiencies", SkillProficiency.values());
        return "recruiter/candidate-form";
    }

    @PostMapping("/{id}/edit")
    @Audited(action = AuditAction.UPDATE, resource = "Candidate")
    public String update(@PathVariable Long id,
                         @ModelAttribute CandidateProfile formData,
                         @RequestParam(required = false) List<String> skillNames,
                         @RequestParam(required = false) List<Integer> skillYears,
                         @RequestParam(required = false) List<SkillProficiency> skillProficiencies,
                         RedirectAttributes redirectAttrs) {
        CandidateProfile existing = candidateService.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
        existing.setFirstName(formData.getFirstName());
        existing.setLastName(formData.getLastName());
        existing.setEmail(formData.getEmail());
        existing.setPhone(formData.getPhone());
        existing.setLocation(formData.getLocation());
        existing.setCurrentTitle(formData.getCurrentTitle());
        existing.setCurrentEmployer(formData.getCurrentEmployer());
        existing.setYearsOfExperience(formData.getYearsOfExperience());
        existing.setEducationLevel(formData.getEducationLevel());
        existing.setSummary(formData.getSummary());
        existing.setPipelineStage(formData.getPipelineStage());
        existing.setTags(formData.getTags());

        existing.getSkills().clear();
        addSkillsToCandidate(existing, skillNames, skillYears, skillProficiencies);

        candidateService.save(existing);
        redirectAttrs.addFlashAttribute("success", "Candidate updated successfully");
        return "redirect:/recruiter/candidates/" + id;
    }

    @PostMapping("/bulk-tag")
    @Audited(action = AuditAction.UPDATE, resource = "CandidateBulkTag")
    public String bulkTag(@RequestParam List<Long> candidateIds,
                          @RequestParam String tag,
                          RedirectAttributes redirectAttrs) {
        candidateService.bulkTag(candidateIds, tag);
        redirectAttrs.addFlashAttribute("success",
                "Tagged " + candidateIds.size() + " candidates with '" + tag + "'");
        return "redirect:/recruiter/candidates";
    }

    private void addSkillsToCandidate(CandidateProfile candidate,
                                       List<String> skillNames,
                                       List<Integer> skillYears,
                                       List<SkillProficiency> skillProficiencies) {
        if (skillNames == null) return;
        for (int i = 0; i < skillNames.size(); i++) {
            String name = skillNames.get(i);
            if (name == null || name.isBlank()) continue;
            Integer years = (skillYears != null && i < skillYears.size()) ? skillYears.get(i) : null;
            SkillProficiency prof = (skillProficiencies != null && i < skillProficiencies.size())
                    ? skillProficiencies.get(i) : null;
            candidate.getSkills().add(new CandidateSkill(candidate, name.trim(), years, prof));
        }
    }
}
