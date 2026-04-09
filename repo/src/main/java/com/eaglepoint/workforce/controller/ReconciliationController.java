package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.ReconciliationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/finance/reconciliation")
@PreAuthorize("hasRole('FINANCE_CLERK') or hasRole('ADMINISTRATOR')")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "ReconciliationExceptions")
    public String exceptions(Model model) {
        model.addAttribute("exceptions", reconciliationService.findAllExceptions());
        model.addAttribute("openCount", reconciliationService.countOpenExceptions());
        return "finance/reconciliation";
    }

    @PostMapping("/resolve/{id}")
    @Audited(action = AuditAction.UPDATE, resource = "ReconciliationResolve")
    public String resolve(@PathVariable Long id,
                           @RequestParam String resolutionNotes,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttrs) {
        reconciliationService.resolveException(id, resolutionNotes, userDetails.getUsername());
        redirectAttrs.addFlashAttribute("success", "Exception resolved");
        return "redirect:/finance/reconciliation";
    }
}
