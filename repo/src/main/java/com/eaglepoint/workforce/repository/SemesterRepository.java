package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {
    Optional<Semester> findByCode(String code);
    boolean existsByCode(String code);
    List<Semester> findByActiveTrue();
}
