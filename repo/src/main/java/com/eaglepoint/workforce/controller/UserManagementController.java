package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.dto.UserCreateRequest;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "UserList")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @GetMapping("/new")
    public String createUserForm(Model model) {
        model.addAttribute("userForm", new UserCreateRequest());
        model.addAttribute("allRoles", RoleName.values());
        return "admin/user-form";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "User")
    public String createUser(@Valid @ModelAttribute("userForm") UserCreateRequest request,
                             BindingResult result, Model model, RedirectAttributes redirectAttrs) {
        if (result.hasErrors()) {
            model.addAttribute("allRoles", RoleName.values());
            return "admin/user-form";
        }

        Set<RoleName> roles = request.getRoles() != null ? request.getRoles() : Set.of();
        userService.createUser(request.getUsername(), request.getPassword(),
                request.getDisplayName(), request.getEmail(), roles);

        redirectAttrs.addFlashAttribute("success", "User created successfully");
        return "redirect:/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        model.addAttribute("allRoles", RoleName.values());
        return "admin/user-form";
    }

    @PostMapping("/{id}/edit")
    @Audited(action = AuditAction.UPDATE, resource = "User")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String displayName,
                             @RequestParam String email,
                             @RequestParam(defaultValue = "true") boolean enabled,
                             @RequestParam(required = false) Set<RoleName> roles,
                             RedirectAttributes redirectAttrs) {
        Set<RoleName> roleSet = roles != null ? roles : Set.of();
        userService.updateUser(id, displayName, email, enabled, roleSet);
        redirectAttrs.addFlashAttribute("success", "User updated successfully");
        return "redirect:/admin/users";
    }
}
