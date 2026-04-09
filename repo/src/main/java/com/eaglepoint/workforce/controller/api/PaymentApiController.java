package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.dto.*;
import com.eaglepoint.workforce.entity.PaymentTransaction;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.PaymentService;
import com.eaglepoint.workforce.service.ResourceAuthorizationService;
import com.eaglepoint.workforce.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@PreAuthorize("hasAnyRole('FINANCE_CLERK','ADMINISTRATOR')")
public class PaymentApiController {

    private final PaymentService paymentService;
    private final UserService userService;
    private final ResourceAuthorizationService authzService;

    public PaymentApiController(PaymentService paymentService, UserService userService,
                                 ResourceAuthorizationService authzService) {
        this.paymentService = paymentService;
        this.userService = userService;
        this.authzService = authzService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "API:PaymentList")
    public ResponseEntity<List<PaymentView>> list(Authentication auth) {
        boolean isAdmin = authzService.isAdmin(auth);
        List<PaymentView> views = paymentService.findAll().stream()
                .map(p -> new PaymentView(p, isAdmin)).toList();
        return ResponseEntity.ok(views);
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "API:PaymentDetail")
    public ResponseEntity<PaymentView> detail(@PathVariable Long id, Authentication auth) {
        PaymentTransaction p = authzService.authorizePayment(id, auth);
        return ResponseEntity.ok(new PaymentView(p, authzService.isAdmin(auth)));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, resource = "API:Payment")
    public ResponseEntity<PaymentView> create(@Valid @RequestBody CreatePaymentRequest req,
                                               Authentication auth) {
        User user = userService.findByUsername(auth.getName()).orElseThrow();
        String key = req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()
                ? req.getIdempotencyKey()
                : PaymentService.generateIdempotencyKey(
                    req.getReferenceNumber(), req.getChannel().name(), req.getAmount().toPlainString());
        PaymentTransaction p = paymentService.recordPayment(key, req.getReferenceNumber(),
                req.getAmount(), req.getChannel(), req.getLocation(), req.getPayerName(),
                req.getDescription(), req.getCheckNumber(), req.getCardLastFour(),
                user.getId(), user.getUsername());
        return ResponseEntity.ok(new PaymentView(p, authzService.isAdmin(auth)));
    }

    @PostMapping("/{id}/refund")
    @Audited(action = AuditAction.CREATE, resource = "API:Refund")
    public ResponseEntity<?> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest req,
                                     Authentication auth) {
        User user = userService.findByUsername(auth.getName()).orElseThrow();
        paymentService.processRefund(id, req.getAmount(), req.getReason(), user.getId(), user.getUsername());
        PaymentTransaction p = paymentService.findById(id).orElseThrow();
        return ResponseEntity.ok(new PaymentView(p, authzService.isAdmin(auth)));
    }
}
