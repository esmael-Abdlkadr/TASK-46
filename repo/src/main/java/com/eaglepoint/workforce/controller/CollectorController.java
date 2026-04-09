package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.CollectorProfile;
import com.eaglepoint.workforce.enums.*;
import com.eaglepoint.workforce.service.CollectorService;
import com.eaglepoint.workforce.service.SiteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;

@Controller
@RequestMapping("/dispatch/collectors")
@PreAuthorize("hasRole('DISPATCH_SUPERVISOR') or hasRole('ADMINISTRATOR')")
public class CollectorController {

    private final CollectorService collectorService;
    private final SiteService siteService;

    public CollectorController(CollectorService collectorService, SiteService siteService) {
        this.collectorService = collectorService;
        this.siteService = siteService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "CollectorList")
    public String list(@RequestParam(required = false) CollectorStatus status, Model model) {
        if (status != null) {
            model.addAttribute("collectors", collectorService.findByStatus(status));
        } else {
            model.addAttribute("collectors", collectorService.findAll());
        }
        model.addAttribute("statuses", CollectorStatus.values());
        model.addAttribute("selectedStatus", status);
        return "dispatch/collectors";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("collector", new CollectorProfile());
        model.addAttribute("statuses", CollectorStatus.values());
        return "dispatch/collector-form";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "Collector")
    public String create(@ModelAttribute CollectorProfile collector, RedirectAttributes redirectAttrs) {
        collectorService.save(collector);
        redirectAttrs.addFlashAttribute("success", "Collector created successfully");
        return "redirect:/dispatch/collectors";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "CollectorDetail")
    public String view(@PathVariable Long id, Model model) {
        CollectorProfile collector = collectorService.findByIdWithShifts(id)
                .orElseThrow(() -> new RuntimeException("Collector not found"));
        model.addAttribute("collector", collector);
        model.addAttribute("shifts", collectorService.getShiftsForCollector(id));
        model.addAttribute("days", DayOfWeekEnum.values());
        model.addAttribute("sites", siteService.findAllActive());
        return "dispatch/collector-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CollectorProfile collector = collectorService.findById(id)
                .orElseThrow(() -> new RuntimeException("Collector not found"));
        model.addAttribute("collector", collector);
        model.addAttribute("statuses", CollectorStatus.values());
        return "dispatch/collector-form";
    }

    @PostMapping("/{id}/edit")
    @Audited(action = AuditAction.UPDATE, resource = "Collector")
    public String update(@PathVariable Long id, @ModelAttribute CollectorProfile formData,
                         RedirectAttributes redirectAttrs) {
        CollectorProfile existing = collectorService.findById(id)
                .orElseThrow(() -> new RuntimeException("Collector not found"));
        existing.setFirstName(formData.getFirstName());
        existing.setLastName(formData.getLastName());
        existing.setEmployeeId(formData.getEmployeeId());
        existing.setPhone(formData.getPhone());
        existing.setEmail(formData.getEmail());
        existing.setZone(formData.getZone());
        existing.setSkills(formData.getSkills());
        existing.setStatus(formData.getStatus());
        existing.setMaxConcurrentJobs(formData.getMaxConcurrentJobs());
        collectorService.save(existing);
        redirectAttrs.addFlashAttribute("success", "Collector updated");
        return "redirect:/dispatch/collectors/" + id;
    }

    @PostMapping("/{id}/shifts/add")
    @Audited(action = AuditAction.CREATE, resource = "WorkShift")
    public String addShift(@PathVariable Long id,
                           @RequestParam DayOfWeekEnum dayOfWeek,
                           @RequestParam String startTime,
                           @RequestParam String endTime,
                           @RequestParam(required = false) Long siteId,
                           RedirectAttributes redirectAttrs) {
        try {
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);
            collectorService.addShift(id, dayOfWeek, start, end, siteId);
            redirectAttrs.addFlashAttribute("success", "Shift added");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dispatch/collectors/" + id;
    }

    @PostMapping("/shifts/{shiftId}/remove")
    @Audited(action = AuditAction.DELETE, resource = "WorkShift")
    public String removeShift(@PathVariable Long shiftId,
                              @RequestParam Long collectorId,
                              RedirectAttributes redirectAttrs) {
        collectorService.removeShift(shiftId);
        redirectAttrs.addFlashAttribute("success", "Shift removed");
        return "redirect:/dispatch/collectors/" + collectorId;
    }
}
