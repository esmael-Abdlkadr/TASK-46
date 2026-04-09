package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.BiometricTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BiometricTemplateRepository extends JpaRepository<BiometricTemplate, Long> {
    Optional<BiometricTemplate> findByUserId(Long userId);
}
