package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.ExportJob;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.ExportStatus;
import com.eaglepoint.workforce.service.ExportService;
import com.eaglepoint.workforce.service.ResourceAuthorizationService;
import com.eaglepoint.workforce.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/exports")
public class ExportController {

    private final ExportService exportService;
    private final UserService userService;
    private final ResourceAuthorizationService authzService;

    public ExportController(ExportService exportService, UserService userService,
                            ResourceAuthorizationService authzService) {
        this.exportService = exportService;
        this.userService = userService;
        this.authzService = authzService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "ExportJobList")
    public String list(Model model, Authentication auth) {
        if (authzService.isAdmin(auth)) {
            model.addAttribute("exports", exportService.findAll());
        } else {
            Long userId = userService.findByUsername(auth.getName()).map(User::getId).orElse(null);
            model.addAttribute("exports", userId != null ? exportService.findByUser(userId) : java.util.List.of());
        }
        return "exports/list";
    }

    @GetMapping("/list")
    @Audited(action = AuditAction.READ, resource = "ExportJobList")
    public String listAlias(Model model, Authentication auth) {
        return list(model, auth);
    }

    @PostMapping("/create")
    @Audited(action = AuditAction.CREATE, resource = "ExportJob")
    public String createExport(@RequestParam String name,
                                @RequestParam String exportType,
                                @RequestParam String fileFormat,
                                @AuthenticationPrincipal UserDetails userDetails,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttrs) {
        Long userId = userService.findByUsername(userDetails.getUsername())
                .map(User::getId).orElse(0L);

        ExportJob job = exportService.queueExport(name, exportType, fileFormat, null, userId);
        exportService.executeExport(job.getId());

        redirectAttrs.addFlashAttribute("success", "Export '" + name + "' queued");
        return "redirect:/exports";
    }

    @GetMapping("/{id}/download")
    @Audited(action = AuditAction.EXPORT, resource = "ExportFile")
    @ResponseBody
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication auth) {
        ExportJob job = authzService.authorizeExportJob(id, auth);

        if (job.getStatus() != ExportStatus.COMPLETED) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] data = exportService.getFileBytes(job);
            String contentType = job.getFileName().endsWith(".csv")
                    ? "text/csv" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + job.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
