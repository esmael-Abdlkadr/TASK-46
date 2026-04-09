package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.PaymentChannel;
import com.eaglepoint.workforce.service.PaymentService;
import com.eaglepoint.workforce.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/finance/payments")
@PreAuthorize("hasRole('FINANCE_CLERK') or hasRole('ADMINISTRATOR')")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "PaymentList")
    public String list(Model model) {
        model.addAttribute("payments", paymentService.findAll());
        model.addAttribute("channels", PaymentChannel.values());
        return "finance/payments";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("channels", PaymentChannel.values());
        return "finance/payment-form";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "Payment")
    public String recordPayment(@RequestParam String referenceNumber,
                                 @RequestParam BigDecimal amount,
                                 @RequestParam PaymentChannel channel,
                                 @RequestParam(required = false) String location,
                                 @RequestParam(required = false) String payerName,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String checkNumber,
                                 @RequestParam(required = false) String cardLastFour,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttrs) {
        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Long userId = user != null ? user.getId() : 0L;

        String idempotencyKey = PaymentService.generateIdempotencyKey(
                referenceNumber, channel.name(), amount.toPlainString());

        try {
            paymentService.recordPayment(idempotencyKey, referenceNumber, amount, channel,
                    location, payerName, description, checkNumber, cardLastFour,
                    userId, userDetails.getUsername());
            redirectAttrs.addFlashAttribute("success", "Payment recorded: " + referenceNumber);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/finance/payments";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "PaymentDetail")
    public String detail(@PathVariable Long id, Model model) {
        PaymentTransaction payment = paymentService.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        model.addAttribute("payment", payment);
        model.addAttribute("refunds", paymentService.getRefundsForPayment(id));
        return "finance/payment-detail";
    }

    @PostMapping("/{id}/refund")
    @Audited(action = AuditAction.CREATE, resource = "Refund")
    public String processRefund(@PathVariable Long id,
                                 @RequestParam BigDecimal refundAmount,
                                 @RequestParam String reason,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttrs) {
        User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
        Long userId = user != null ? user.getId() : 0L;

        try {
            paymentService.processRefund(id, refundAmount, reason, userId, userDetails.getUsername());
            redirectAttrs.addFlashAttribute("success", "Refund of " + refundAmount + " processed");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/finance/payments/" + id;
    }
}
