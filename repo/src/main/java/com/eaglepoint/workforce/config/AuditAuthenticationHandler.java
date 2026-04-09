package com.eaglepoint.workforce.config;

import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.UserRepository;
import com.eaglepoint.workforce.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class AuditAuthenticationHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final AuditService auditService;
    private final UserRepository userRepository;

    public AuditAuthenticationHandler(AuditService auditService, UserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String workstationId = request.getRemoteAddr();

        User user = userRepository.findByUsername(username).orElse(null);
        Long userId = user != null ? user.getId() : null;

        auditService.log(userId, username, AuditAction.LOGIN_SUCCESS, "Authentication",
                null, "Successful login", workstationId);

        String redirectUrl = determineTargetUrl(authentication.getAuthorities());
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String workstationId = request.getRemoteAddr();

        auditService.log(null, username, AuditAction.LOGIN_FAILURE, "Authentication",
                null, "Failed login: " + exception.getMessage(), workstationId);

        response.sendRedirect(request.getContextPath() + "/login?error");
    }

    private String determineTargetUrl(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_" + RoleName.ADMINISTRATOR.name())) {
                return "/admin/dashboard";
            }
            if (role.equals("ROLE_" + RoleName.RECRUITER.name())) {
                return "/recruiter/dashboard";
            }
            if (role.equals("ROLE_" + RoleName.DISPATCH_SUPERVISOR.name())) {
                return "/dispatch/dashboard";
            }
            if (role.equals("ROLE_" + RoleName.FINANCE_CLERK.name())) {
                return "/finance/dashboard";
            }
        }
        return "/login";
    }
}
