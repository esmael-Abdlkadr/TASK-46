package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.AsyncJobService;
import com.eaglepoint.workforce.service.FaceRecognitionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/jobs")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AsyncJobController {

    private final AsyncJobService asyncJobService;
    private final FaceRecognitionService faceService;

    public AsyncJobController(AsyncJobService asyncJobService,
                               FaceRecognitionService faceService) {
        this.asyncJobService = asyncJobService;
        this.faceService = faceService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "AsyncJobQueue")
    public String list(Model model) {
        model.addAttribute("jobs", asyncJobService.findAll());
        model.addAttribute("health", asyncJobService.getHealthStatus());
        model.addAttribute("faceServiceUp", faceService.isServiceAvailable());
        return "admin/jobs";
    }
}
