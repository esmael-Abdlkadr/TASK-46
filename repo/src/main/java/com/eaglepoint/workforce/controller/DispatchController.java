package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.DispatchAssignment;
import com.eaglepoint.workforce.entity.SiteProfile;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.DispatchStatus;
import com.eaglepoint.workforce.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/dispatch/assignments")
@PreAuthorize("hasRole('DISPATCH_SUPERVISOR') or hasRole('ADMINISTRATOR')")
public class DispatchController {

    private final DispatchService dispatchService;
    private final SiteService siteService;
    private final CollectorService collectorService;
    private final UserService userService;

    public DispatchController(DispatchService dispatchService, SiteService siteService,
                               CollectorService collectorService, UserService userService) {
        this.dispatchService = dispatchService;
        this.siteService = siteService;
        this.collectorService = collectorService;
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "DispatchAssignmentList")
    public String list(@RequestParam(required = false) DispatchStatus status, Model model) {
        List<DispatchAssignment> assignments;
        if (status != null) {
            assignments = dispatchService.findByStatusWithDetails(List.of(status));
        } else {
            assignments = dispatchService.findAllWithDetails();
        }
        model.addAttribute("assignments", assignments);
        model.addAttribute("statuses", DispatchStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pendingCount", dispatchService.countByStatus(DispatchStatus.PENDING));
        model.addAttribute("offeredCount", dispatchService.countByStatus(DispatchStatus.OFFERED));
        model.addAttribute("acceptedCount", dispatchService.countByStatus(DispatchStatus.ACCEPTED));
        return "dispatch/assignments";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("sites", siteService.findAllActive());
        return "dispatch/assignment-form";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "DispatchAssignment")
    public String create(@RequestParam Long siteId,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String scheduledStart,
                         @RequestParam(required = false) String scheduledEnd,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttrs) {
        SiteProfile site = siteService.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        Long userId = userService.findByUsername(userDetails.getUsername())
                .map(User::getId).orElse(0L);

        LocalDateTime start = parseDateTime(scheduledStart);
        LocalDateTime end = parseDateTime(scheduledEnd);

        try {
            dispatchService.createAssignment(site, title, description, start, end, userId);
            redirectAttrs.addFlashAttribute("success", "Assignment created" +
                    (site.getDispatchMode().name().equals("ASSIGNED_ORDER") ? " and auto-dispatched" : ""));
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dispatch/assignments";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "DispatchAssignmentDetail")
    public String view(@PathVariable Long id, Model model) {
        DispatchAssignment assignment = dispatchService.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        model.addAttribute("assignment", assignment);
        model.addAttribute("offerHistory", dispatchService.getOfferHistory(id));
        model.addAttribute("availableCollectors", collectorService.findAvailable());
        return "dispatch/assignment-detail";
    }

    @PostMapping("/{id}/offer")
    @Audited(action = AuditAction.UPDATE, resource = "DispatchOffer")
    public String offerToCollector(@PathVariable Long id, @RequestParam Long collectorId,
                                    RedirectAttributes redirectAttrs) {
        try {
            dispatchService.offerToCollector(id, collectorId);
            redirectAttrs.addFlashAttribute("success", "Job offered to collector");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dispatch/assignments/" + id;
    }

    @PostMapping("/{id}/accept")
    @Audited(action = AuditAction.UPDATE, resource = "DispatchAccept")
    public String accept(@PathVariable Long id, @RequestParam Long collectorId,
                          RedirectAttributes redirectAttrs) {
        try {
            dispatchService.acceptAssignment(id, collectorId);
            redirectAttrs.addFlashAttribute("success", "Assignment accepted");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dispatch/assignments/" + id;
    }

    @PostMapping("/{id}/decline")
    @Audited(action = AuditAction.UPDATE, resource = "DispatchDecline")
    public String decline(@PathVariable Long id, @RequestParam Long collectorId,
                           RedirectAttributes redirectAttrs) {
        try {
            dispatchService.declineAssignment(id, collectorId);
            redirectAttrs.addFlashAttribute("success", "Assignment declined, auto-redispatching...");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dispatch/assignments/" + id;
    }

    @PostMapping("/{id}/grab")
    @Audited(action = AuditAction.UPDATE, resource = "DispatchGrab")
    public String grab(@PathVariable Long id, @RequestParam Long collectorId,
                        RedirectAttributes redirectAttrs) {
        try {
            dispatchService.grabJob(id, collectorId);
            redirectAttrs.addFlashAttribute("success", "Job grabbed by collector");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dispatch/assignments/" + id;
    }

    @PostMapping("/{id}/complete")
    @Audited(action = AuditAction.UPDATE, resource = "DispatchComplete")
    public String complete(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            dispatchService.completeAssignment(id);
            redirectAttrs.addFlashAttribute("success", "Assignment completed");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dispatch/assignments/" + id;
    }

    @PostMapping("/{id}/cancel")
    @Audited(action = AuditAction.UPDATE, resource = "DispatchCancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            dispatchService.cancelAssignment(id);
            redirectAttrs.addFlashAttribute("success", "Assignment cancelled");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dispatch/assignments/" + id;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
