package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.ExportJob;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.ExportService;
import com.eaglepoint.workforce.service.ResourceAuthorizationService;
import com.eaglepoint.workforce.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportApiController {

    private final ExportService exportService;
    private final UserService userService;
    private final ResourceAuthorizationService authzService;

    public ExportApiController(ExportService exportService, UserService userService,
                                ResourceAuthorizationService authzService) {
        this.exportService = exportService;
        this.userService = userService;
        this.authzService = authzService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "API:ExportList")
    public ResponseEntity<List<ExportJob>> list(Authentication auth) {
        Long userId = authzService.resolveUserId(auth);
        List<ExportJob> jobs = authzService.isAdmin(auth)
                ? exportService.findAll() : exportService.findByUser(userId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "API:ExportDetail")
    public ResponseEntity<ExportJob> detail(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(authzService.authorizeExportJob(id, auth));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, resource = "API:Export")
    public ResponseEntity<ExportJob> create(@RequestBody Map<String, String> req, Authentication auth) {
        User user = userService.findByUsername(auth.getName()).orElseThrow();
        ExportJob job = exportService.queueExport(
                req.getOrDefault("name", "Export"),
                req.getOrDefault("exportType", "candidates"),
                req.getOrDefault("fileFormat", "csv"),
                req.get("searchCriteria"), user.getId());
        exportService.executeExport(job.getId());
        return ResponseEntity.ok(job);
    }
}
