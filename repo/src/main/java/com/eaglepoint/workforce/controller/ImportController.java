package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.ImportJob;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.ImportStatus;
import com.eaglepoint.workforce.service.ImportService;
import com.eaglepoint.workforce.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/imports")
public class ImportController {

    private final ImportService importService;
    private final UserService userService;

    public ImportController(ImportService importService, UserService userService) {
        this.importService = importService;
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "ImportJobList")
    public String list(Model model) {
        model.addAttribute("imports", importService.findAll());
        return "imports/list";
    }

    @PostMapping("/upload")
    @Audited(action = AuditAction.CREATE, resource = "ImportJob")
    public String upload(@RequestParam("file") MultipartFile file,
                          @RequestParam String importType,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttrs) {
        Long userId = userService.findByUsername(userDetails.getUsername())
                .map(User::getId).orElse(0L);

        try {
            String originalName = file.getOriginalFilename();
            if (originalName == null || (!originalName.endsWith(".csv")
                    && !originalName.endsWith(".xlsx") && !originalName.endsWith(".xls"))) {
                redirectAttrs.addFlashAttribute("error", "Only .csv, .xlsx, or .xls files are supported");
                return "redirect:/imports";
            }

            ImportJob job = importService.prepareImport(file, importType, userId);

            if (job.getStatus() == ImportStatus.DUPLICATE) {
                redirectAttrs.addFlashAttribute("error",
                        "Duplicate file detected. This file has already been imported.");
                return "redirect:/imports";
            }

            importService.executeImport(job.getId());
            redirectAttrs.addFlashAttribute("success",
                    "Import of '" + originalName + "' queued for processing");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }

        return "redirect:/imports";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "ImportJobDetail")
    public String detail(@PathVariable Long id, Model model) {
        ImportJob job = importService.findById(id)
                .orElseThrow(() -> new RuntimeException("Import job not found"));
        model.addAttribute("job", job);
        return "imports/detail";
    }
}
