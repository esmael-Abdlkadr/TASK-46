package com.eaglepoint.workforce.audit;

import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.repository.UserRepository;
import com.eaglepoint.workforce.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;
    private final UserRepository userRepository;

    public AuditAspect(AuditService auditService, UserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @AfterReturning("@annotation(audited)")
    public void audit(JoinPoint joinPoint, Audited audited) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        Long userId = user != null ? user.getId() : null;

        String workstationId = "unknown";
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            workstationId = request.getRemoteAddr();
        }

        String detail = joinPoint.getSignature().toShortString();

        auditService.log(userId, username, audited.action(), audited.resource(),
                null, detail, workstationId);
    }
}
