package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.StaffingClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StaffingClassRepository extends JpaRepository<StaffingClass, Long> {
    Optional<StaffingClass> findByCode(String code);
    boolean existsByCode(String code);
    List<StaffingClass> findByDepartmentId(Long departmentId);
    List<StaffingClass> findByActiveTrue();
}
