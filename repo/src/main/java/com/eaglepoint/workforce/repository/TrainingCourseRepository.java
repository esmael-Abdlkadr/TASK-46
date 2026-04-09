package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.TrainingCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrainingCourseRepository extends JpaRepository<TrainingCourse, Long> {
    Optional<TrainingCourse> findByCode(String code);
    boolean existsByCode(String code);
    List<TrainingCourse> findByDepartmentId(Long departmentId);
    List<TrainingCourse> findByActiveTrue();
}
