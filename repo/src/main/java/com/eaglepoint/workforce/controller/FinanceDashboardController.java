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
@RequestMapping("/finance")
@PreAuthorize("hasRole('FINANCE_CLERK')")
public class FinanceDashboardController {

    private final DashboardService dashboardService;

    public FinanceDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @Audited(action = AuditAction.READ, resource = "FinanceDashboard")
    public String dashboard(Model model) {
        DashboardData data = dashboardService.getDashboardData();
        model.addAttribute("dashboard", data);
        return "finance/dashboard";
    }
}
