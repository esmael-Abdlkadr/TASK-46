package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MasterDataService {

    private final DepartmentRepository departmentRepo;
    private final StaffingClassRepository classRepo;
    private final TrainingCourseRepository courseRepo;
    private final SemesterRepository semesterRepo;

    public MasterDataService(DepartmentRepository departmentRepo,
                              StaffingClassRepository classRepo,
                              TrainingCourseRepository courseRepo,
                              SemesterRepository semesterRepo) {
        this.departmentRepo = departmentRepo;
        this.classRepo = classRepo;
        this.courseRepo = courseRepo;
        this.semesterRepo = semesterRepo;
    }

    // Departments
    @Transactional(readOnly = true)
    public List<Department> findAllDepartments() { return departmentRepo.findAll(); }

    @Transactional(readOnly = true)
    public Optional<Department> findDepartmentById(Long id) { return departmentRepo.findById(id); }

    @Transactional
    public Department saveDepartment(Department dept) { return departmentRepo.save(dept); }

    // Staffing Classes
    @Transactional(readOnly = true)
    public List<StaffingClass> findAllClasses() { return classRepo.findAll(); }

    @Transactional(readOnly = true)
    public Optional<StaffingClass> findClassById(Long id) { return classRepo.findById(id); }

    @Transactional
    public StaffingClass saveClass(StaffingClass sc) { return classRepo.save(sc); }

    @Transactional(readOnly = true)
    public List<StaffingClass> findClassesByDepartment(Long deptId) { return classRepo.findByDepartmentId(deptId); }

    // Training Courses
    @Transactional(readOnly = true)
    public List<TrainingCourse> findAllCourses() { return courseRepo.findAll(); }

    @Transactional(readOnly = true)
    public Optional<TrainingCourse> findCourseById(Long id) { return courseRepo.findById(id); }

    @Transactional
    public TrainingCourse saveCourse(TrainingCourse course) { return courseRepo.save(course); }

    // Semesters
    @Transactional(readOnly = true)
    public List<Semester> findAllSemesters() { return semesterRepo.findAll(); }

    @Transactional(readOnly = true)
    public Optional<Semester> findSemesterById(Long id) { return semesterRepo.findById(id); }

    @Transactional
    public Semester saveSemester(Semester sem) { return semesterRepo.save(sem); }
}
