package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.dto.BatchMoveResult;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.PipelineStage;
import com.eaglepoint.workforce.service.CandidateService;
import com.eaglepoint.workforce.service.PipelineService;
import com.eaglepoint.workforce.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter/pipeline")
@PreAuthorize("hasRole('RECRUITER') or hasRole('ADMINISTRATOR')")
public class PipelineController {

    private final PipelineService pipelineService;
    private final CandidateService candidateService;
    private final UserService userService;

    public PipelineController(PipelineService pipelineService,
                               CandidateService candidateService,
                               UserService userService) {
        this.pipelineService = pipelineService;
        this.candidateService = candidateService;
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "Pipeline")
    public String pipeline(Model model) {
        model.addAttribute("stageCounts", candidateService.getPipelineStageCounts());
        model.addAttribute("stages", PipelineStage.values());
        return "recruiter/pipeline";
    }

    @PostMapping("/batch-move")
    @Audited(action = AuditAction.UPDATE, resource = "PipelineBatchMove")
    public String batchMove(@RequestParam List<Long> candidateIds,
                            @RequestParam PipelineStage targetStage,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttrs) {
        Long userId = userService.findByUsername(userDetails.getUsername())
                .map(User::getId).orElse(0L);

        BatchMoveResult result = pipelineService.batchMoveStage(candidateIds, targetStage, userId);

        redirectAttrs.addFlashAttribute("success",
                "Moved " + result.getMovedCount() + " candidates to " + targetStage.getDisplayName());
        redirectAttrs.addFlashAttribute("batchId", result.getBatchId());
        redirectAttrs.addFlashAttribute("undoDeadline", result.getUndoDeadline().toString());
        return "redirect:/recruiter/candidates";
    }

    @PostMapping("/undo/{batchId}")
    @Audited(action = AuditAction.UPDATE, resource = "PipelineUndo")
    public String undoBatchMove(@PathVariable String batchId,
                                 RedirectAttributes redirectAttrs) {
        try {
            int count = pipelineService.undoBatchMove(batchId);
            redirectAttrs.addFlashAttribute("success",
                    "Undo successful: " + count + " candidates reverted to previous stage");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recruiter/candidates";
    }
}
