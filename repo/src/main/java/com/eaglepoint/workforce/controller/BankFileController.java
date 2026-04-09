package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.BankFileService;
import com.eaglepoint.workforce.service.ReconciliationService;
import com.eaglepoint.workforce.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/finance/bank-files")
@PreAuthorize("hasRole('FINANCE_CLERK') or hasRole('ADMINISTRATOR')")
public class BankFileController {

    private final BankFileService bankFileService;
    private final ReconciliationService reconciliationService;
    private final UserService userService;

    public BankFileController(BankFileService bankFileService,
                               ReconciliationService reconciliationService,
                               UserService userService) {
        this.bankFileService = bankFileService;
        this.reconciliationService = reconciliationService;
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "BankFileList")
    public String list(Model model) {
        model.addAttribute("imports", bankFileService.findAllImports());
        return "finance/bank-files";
    }

    @PostMapping("/upload")
    @Audited(action = AuditAction.CREATE, resource = "BankFileImport")
    public String upload(@RequestParam("file") MultipartFile file,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttrs) {
        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Long userId = user != null ? user.getId() : 0L;

        try {
            var bfi = bankFileService.importBankFile(file, userId, userDetails.getUsername());
            redirectAttrs.addFlashAttribute("success",
                    "Imported " + bfi.getTotalEntries() + " bank entries from " + bfi.getFileName());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/finance/bank-files";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "BankFileDetail")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("bankImport", bankFileService.findImportById(id)
                .orElseThrow(() -> new RuntimeException("Bank file import not found")));
        model.addAttribute("entries", bankFileService.findEntriesByImport(id));
        return "finance/bank-file-detail";
    }

    @PostMapping("/{id}/reconcile")
    @Audited(action = AuditAction.UPDATE, resource = "Reconciliation")
    public String reconcile(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        int matched = reconciliationService.runReconciliation(id);
        redirectAttrs.addFlashAttribute("success",
                "Reconciliation complete: " + matched + " entries matched");
        return "redirect:/finance/bank-files/" + id;
    }
}
