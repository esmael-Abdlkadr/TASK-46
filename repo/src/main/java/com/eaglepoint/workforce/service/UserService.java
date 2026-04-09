package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.Role;
import com.eaglepoint.workforce.entity.User;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.RoleRepository;
import com.eaglepoint.workforce.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User createUser(String username, String password, String displayName,
                           String email, Set<RoleName> roleNames) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setEmail(email);

        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            roleRepository.findByName(roleName).ifPresent(roles::add);
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, String displayName, String email,
                           boolean enabled, Set<RoleName> roleNames) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setEnabled(enabled);

        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            roleRepository.findByName(roleName).ifPresent(roles::add);
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
