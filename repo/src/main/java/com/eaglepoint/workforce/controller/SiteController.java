package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.SiteProfile;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.DispatchMode;
import com.eaglepoint.workforce.service.SiteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dispatch/sites")
@PreAuthorize("hasRole('DISPATCH_SUPERVISOR') or hasRole('ADMINISTRATOR')")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "SiteList")
    public String list(Model model) {
        model.addAttribute("sites", siteService.findAll());
        return "dispatch/sites";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("site", new SiteProfile());
        model.addAttribute("dispatchModes", DispatchMode.values());
        return "dispatch/site-form";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "Site")
    public String create(@ModelAttribute SiteProfile site, RedirectAttributes redirectAttrs) {
        siteService.save(site);
        redirectAttrs.addFlashAttribute("success", "Site created successfully");
        return "redirect:/dispatch/sites";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "SiteDetail")
    public String view(@PathVariable Long id, Model model) {
        SiteProfile site = siteService.findById(id)
                .orElseThrow(() -> new RuntimeException("Site not found"));
        model.addAttribute("site", site);
        return "dispatch/site-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SiteProfile site = siteService.findById(id)
                .orElseThrow(() -> new RuntimeException("Site not found"));
        model.addAttribute("site", site);
        model.addAttribute("dispatchModes", DispatchMode.values());
        return "dispatch/site-form";
    }

    @PostMapping("/{id}/edit")
    @Audited(action = AuditAction.UPDATE, resource = "Site")
    public String update(@PathVariable Long id, @ModelAttribute SiteProfile formData,
                         RedirectAttributes redirectAttrs) {
        SiteProfile existing = siteService.findById(id)
                .orElseThrow(() -> new RuntimeException("Site not found"));
        existing.setName(formData.getName());
        existing.setAddress(formData.getAddress());
        existing.setZone(formData.getZone());
        existing.setCapacityLimit(formData.getCapacityLimit());
        existing.setDispatchMode(formData.getDispatchMode());
        existing.setContactName(formData.getContactName());
        existing.setContactPhone(formData.getContactPhone());
        existing.setNotes(formData.getNotes());
        existing.setActive(formData.isActive());
        siteService.save(existing);
        redirectAttrs.addFlashAttribute("success", "Site updated");
        return "redirect:/dispatch/sites/" + id;
    }
}
