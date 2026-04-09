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
        if (!userRepository.existsByUsername("admin")) {
            Role adminRole = roleRepository.findByName(RoleName.ADMINISTRATOR)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMINISTRATOR, "System Administrator")));
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
            adminUser.setDisplayName("System Administrator");
            adminUser.setEmail("admin@eaglepoint.local");
            adminUser.setRoles(Set.of(adminRole));
            userRepository.save(adminUser);
        }
    }
}
