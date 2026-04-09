package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.ExportJob;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.ExportStatus;
import com.eaglepoint.workforce.service.ExportService;
import com.eaglepoint.workforce.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public ExportController(ExportService exportService, UserService userService) {
        this.exportService = exportService;
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "ExportJobList")
    public String list(Model model) {
        model.addAttribute("exports", exportService.findAll());
        return "exports/list";
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
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        ExportJob job = exportService.findById(id)
                .orElseThrow(() -> new RuntimeException("Export job not found"));

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
