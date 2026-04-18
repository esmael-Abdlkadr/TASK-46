package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.entity.Role;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.RoleRepository;
import com.eaglepoint.workforce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers write endpoints in /admin/users: list, new form, create, edit form, update.
 * Verifies RBAC blocking and successful admin operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminWriteControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;

    private User existingUser;

    @BeforeEach
    void setup() {
        Role recruiterRole = roleRepo.findByName(RoleName.RECRUITER).orElseGet(() -> {
            Role r = new Role();
            r.setName(RoleName.RECRUITER);
            return roleRepo.save(r);
        });
        existingUser = userRepo.findByUsername("adminWriteTarget").orElseGet(() -> {
            User u = new User();
            u.setUsername("adminWriteTarget");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Write Target");
            u.setEmail("writetarget@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessNewUserForm() throws Exception {
        mockMvc.perform(get("/admin/users/new"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanCreateUser() throws Exception {
        mockMvc.perform(post("/admin/users/new").with(csrf())
                        .param("username", "newTestUser99")
                        .param("password", "password123")
                        .param("displayName", "New Test User")
                        .param("email", "newtestuser99@test.com")
                        .param("roles", "RECRUITER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanAccessEditUserForm() throws Exception {
        mockMvc.perform(get("/admin/users/" + existingUser.getId() + "/edit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void adminCanUpdateUser() throws Exception {
        mockMvc.perform(post("/admin/users/" + existingUser.getId() + "/edit").with(csrf())
                        .param("displayName", "Updated Display Name")
                        .param("email", "updated@test.com")
                        .param("enabled", "true")
                        .param("roles", "RECRUITER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void nonAdmin_cannotAccessUserList() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE_CLERK")
    void financeClerk_cannotCreateUser() throws Exception {
        mockMvc.perform(post("/admin/users/new").with(csrf())
                        .param("username", "blockedUser")
                        .param("password", "password123")
                        .param("displayName", "Blocked")
                        .param("email", "blocked@test.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DISPATCH_SUPERVISOR")
    void dispatchSupervisor_cannotEditUser() throws Exception {
        mockMvc.perform(post("/admin/users/" + existingUser.getId() + "/edit").with(csrf())
                        .param("displayName", "Hacked")
                        .param("email", "hacked@test.com")
                        .param("enabled", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_cannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }
}
