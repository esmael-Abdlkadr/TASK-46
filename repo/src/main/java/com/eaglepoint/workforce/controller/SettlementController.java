package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.SettlementService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@Controller
@RequestMapping("/finance/settlement")
@PreAuthorize("hasRole('FINANCE_CLERK') or hasRole('ADMINISTRATOR')")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "SettlementPage")
    public String page(Model model) {
        model.addAttribute("locations", settlementService.getLocations());
        return "finance/settlement";
    }

    @GetMapping("/download")
    @Audited(action = AuditAction.EXPORT, resource = "SettlementStatement")
    @ResponseBody
    public ResponseEntity<byte[]> download(@RequestParam String location,
                                            @RequestParam String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        String csv = settlementService.generateMonthlySettlementCsv(location, ym);
        String fileName = "settlement_" + location.replace(" ", "_") + "_" + yearMonth + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
