package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.dto.AuditLogFilter;
import com.eaglepoint.workforce.entity.AuditLog;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String viewAuditLog(@ModelAttribute AuditLogFilter filter,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "25") int size,
                               Model model) {
        Page<AuditLog> logs;
        if (hasFilters(filter)) {
            logs = auditService.search(filter.getUserId(), filter.getAction(),
                    filter.getDateFrom(), filter.getDateTo(),
                    filter.getWorkstationId(), PageRequest.of(page, size));
        } else {
            logs = auditService.findAll(PageRequest.of(page, size));
        }

        model.addAttribute("logs", logs);
        model.addAttribute("filter", filter);
        model.addAttribute("actions", AuditAction.values());
        return "admin/audit";
    }

    private boolean hasFilters(AuditLogFilter filter) {
        return filter.getUserId() != null || filter.getAction() != null
                || filter.getDateFrom() != null || filter.getDateTo() != null
                || (filter.getWorkstationId() != null && !filter.getWorkstationId().isBlank());
    }
}
