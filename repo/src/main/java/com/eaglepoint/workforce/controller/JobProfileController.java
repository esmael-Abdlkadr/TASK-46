package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.JobProfile;
import com.eaglepoint.workforce.entity.JobProfileCriterion;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.CriterionType;
import com.eaglepoint.workforce.service.JobProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter/job-profiles")
@PreAuthorize("hasRole('RECRUITER') or hasRole('ADMINISTRATOR')")
public class JobProfileController {

    private final JobProfileService jobProfileService;

    public JobProfileController(JobProfileService jobProfileService) {
        this.jobProfileService = jobProfileService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "JobProfileList")
    public String list(Model model) {
        model.addAttribute("jobProfiles", jobProfileService.findAll());
        return "recruiter/job-profiles";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("jobProfile", new JobProfile());
        model.addAttribute("criterionTypes", CriterionType.values());
        return "recruiter/job-profile-form";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "JobProfile")
    public String create(@ModelAttribute JobProfile jobProfile,
                         @RequestParam(required = false) List<String> criteriaSkillNames,
                         @RequestParam(required = false) List<Integer> criteriaMinYears,
                         @RequestParam(required = false) List<CriterionType> criteriaTypes,
                         @RequestParam(required = false) List<Double> criteriaWeights,
                         RedirectAttributes redirectAttrs) {
        addCriteriaToProfile(jobProfile, criteriaSkillNames, criteriaMinYears, criteriaTypes, criteriaWeights);
        jobProfileService.save(jobProfile);
        redirectAttrs.addFlashAttribute("success", "Job profile created successfully");
        return "redirect:/recruiter/job-profiles";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "JobProfileDetail")
    public String view(@PathVariable Long id, Model model) {
        JobProfile jp = jobProfileService.findByIdWithCriteria(id)
                .orElseThrow(() -> new RuntimeException("Job profile not found"));
        model.addAttribute("jobProfile", jp);
        return "recruiter/job-profile-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        JobProfile jp = jobProfileService.findByIdWithCriteria(id)
                .orElseThrow(() -> new RuntimeException("Job profile not found"));
        model.addAttribute("jobProfile", jp);
        model.addAttribute("criterionTypes", CriterionType.values());
        return "recruiter/job-profile-form";
    }

    @PostMapping("/{id}/edit")
    @Audited(action = AuditAction.UPDATE, resource = "JobProfile")
    public String update(@PathVariable Long id,
                         @ModelAttribute JobProfile formData,
                         @RequestParam(required = false) List<String> criteriaSkillNames,
                         @RequestParam(required = false) List<Integer> criteriaMinYears,
                         @RequestParam(required = false) List<CriterionType> criteriaTypes,
                         @RequestParam(required = false) List<Double> criteriaWeights,
                         RedirectAttributes redirectAttrs) {
        JobProfile existing = jobProfileService.findByIdWithCriteria(id)
                .orElseThrow(() -> new RuntimeException("Job profile not found"));
        existing.setTitle(formData.getTitle());
        existing.setDepartment(formData.getDepartment());
        existing.setLocation(formData.getLocation());
        existing.setDescription(formData.getDescription());
        existing.setMinYearsExperience(formData.getMinYearsExperience());
        existing.setEducationRequirement(formData.getEducationRequirement());
        existing.setActive(formData.isActive());

        existing.getCriteria().clear();
        addCriteriaToProfile(existing, criteriaSkillNames, criteriaMinYears, criteriaTypes, criteriaWeights);

        jobProfileService.save(existing);
        redirectAttrs.addFlashAttribute("success", "Job profile updated successfully");
        return "redirect:/recruiter/job-profiles/" + id;
    }

    private void addCriteriaToProfile(JobProfile profile,
                                       List<String> skillNames,
                                       List<Integer> minYears,
                                       List<CriterionType> types,
                                       List<Double> weights) {
        if (skillNames == null) return;
        for (int i = 0; i < skillNames.size(); i++) {
            String name = skillNames.get(i);
            if (name == null || name.isBlank()) continue;
            Integer years = (minYears != null && i < minYears.size()) ? minYears.get(i) : null;
            CriterionType type = (types != null && i < types.size()) ? types.get(i) : CriterionType.REQUIRED;
            Double weight = (weights != null && i < weights.size()) ? weights.get(i) : 1.0;
            profile.getCriteria().add(new JobProfileCriterion(profile, name.trim(), years, type, weight));
        }
    }
}
