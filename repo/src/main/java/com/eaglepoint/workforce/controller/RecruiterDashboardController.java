package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.dto.DashboardData;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/recruiter")
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterDashboardController {

    private final DashboardService dashboardService;

    public RecruiterDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @Audited(action = AuditAction.READ, resource = "RecruiterDashboard")
    public String dashboard(Model model) {
        DashboardData data = dashboardService.getDashboardData();
        model.addAttribute("dashboard", data);
        return "recruiter/dashboard";
    }
}
