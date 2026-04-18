package com.eaglepoint.workforce.init;

import com.eaglepoint.workforce.entity.Role;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.RoleRepository;
import com.eaglepoint.workforce.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role adminRole = roleRepository.findByName(RoleName.ADMINISTRATOR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMINISTRATOR, "System Administrator")));
        Role recruiterRole = roleRepository.findByName(RoleName.RECRUITER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.RECRUITER, "Recruiter")));
        Role dispatchRole = roleRepository.findByName(RoleName.DISPATCH_SUPERVISOR)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.DISPATCH_SUPERVISOR, "Dispatch Supervisor")));
        Role financeRole = roleRepository.findByName(RoleName.FINANCE_CLERK)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.FINANCE_CLERK, "Finance Clerk")));

        ensureUser("admin", "admin123", "System Administrator", "admin@eaglepoint.local", Set.of(adminRole));
        ensureUser("recruiter", "recruiter123", "Demo Recruiter", "recruiter@eaglepoint.local", Set.of(recruiterRole));
        ensureUser("dispatch", "dispatch123", "Demo Dispatch", "dispatch@eaglepoint.local", Set.of(dispatchRole));
        ensureUser("finance", "finance123", "Demo Finance", "finance@eaglepoint.local", Set.of(financeRole));
    }

    private void ensureUser(String username, String plainPassword, String displayName, String email, Set<Role> roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(plainPassword));
        u.setDisplayName(displayName);
        u.setEmail(email);
        u.setRoles(roles);
        userRepository.save(u);
    }
}
