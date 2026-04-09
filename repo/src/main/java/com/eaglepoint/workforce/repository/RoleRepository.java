package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.Role;
import com.eaglepoint.workforce.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
