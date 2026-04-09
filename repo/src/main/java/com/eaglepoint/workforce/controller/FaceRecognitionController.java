package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.FaceRecognitionClient;
import com.eaglepoint.workforce.service.FaceRecognitionService;
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
@RequestMapping("/admin/face-recognition")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class FaceRecognitionController {

    private final FaceRecognitionService faceService;
    private final UserService userService;

    public FaceRecognitionController(FaceRecognitionService faceService, UserService userService) {
        this.faceService = faceService;
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "FaceRecognition")
    public String index(Model model) {
        model.addAttribute("serviceAvailable", faceService.isServiceAvailable());
        model.addAttribute("users", userService.findAll());
        return "admin/face-recognition";
    }

    @PostMapping("/enroll")
    @Audited(action = AuditAction.CREATE, resource = "BiometricEnrollment")
    public String enroll(@RequestParam Long userId,
                          @RequestParam("image") MultipartFile image,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttrs) {
        try {
            User admin = userService.findByUsername(userDetails.getUsername()).orElse(null);
            Long adminId = admin != null ? admin.getId() : 0L;

            faceService.submitAsyncExtraction(image.getBytes(), userId, adminId, userDetails.getUsername());
            redirectAttrs.addFlashAttribute("success",
                    "Face enrollment submitted as async job for user " + userId);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/face-recognition";
    }

    @PostMapping("/verify")
    @Audited(action = AuditAction.READ, resource = "FaceVerification")
    public String verify(@RequestParam Long userId,
                          @RequestParam("image") MultipartFile image,
                          Model model, RedirectAttributes redirectAttrs) {
        try {
            FaceRecognitionClient.FaceMatchResult result = faceService.verifyFace(userId, image.getBytes());
            redirectAttrs.addFlashAttribute("verifyResult", result);
            redirectAttrs.addFlashAttribute("verifyUserId", userId);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/face-recognition";
    }
}
