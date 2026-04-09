package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MasterDataServiceTest {

    @Autowired
    private MasterDataService masterDataService;

    @Autowired
    private DepartmentRepository departmentRepo;

    @Autowired
    private StaffingClassRepository classRepo;

    @Autowired
    private TrainingCourseRepository courseRepo;

    @Autowired
    private SemesterRepository semesterRepo;

    @Test
    void createDepartment() {
        Department dept = new Department();
        dept.setCode("HR");
        dept.setName("Human Resources");
        dept.setHeadName("Bob");
        dept = masterDataService.saveDepartment(dept);
        assertNotNull(dept.getId());
    }

    @Test
    void departmentCodeUniqueness() {
        Department d1 = new Department();
        d1.setCode("UNIQUE1");
        d1.setName("Dept 1");
        departmentRepo.save(d1);

        Department d2 = new Department();
        d2.setCode("UNIQUE1");
        d2.setName("Dept 2");
        assertThrows(DataIntegrityViolationException.class, () -> {
            departmentRepo.saveAndFlush(d2);
        });
    }

    @Test
    void staffingClassWithDepartmentRef() {
        Department dept = new Department();
        dept.setCode("IT");
        dept.setName("Information Technology");
        dept = departmentRepo.save(dept);

        StaffingClass sc = new StaffingClass();
        sc.setCode("SC-IT-01");
        sc.setName("IT Class 1");
        sc.setDepartment(dept);
        sc.setMaxHeadcount(30);
        sc = masterDataService.saveClass(sc);

        assertNotNull(sc.getId());
        assertEquals(dept.getId(), sc.getDepartment().getId());
    }

    @Test
    void trainingCourseCreation() {
        TrainingCourse tc = new TrainingCourse();
        tc.setCode("JAVA101");
        tc.setName("Java Fundamentals");
        tc.setCreditHours(40);
        tc.setMandatory(true);
        tc = masterDataService.saveCourse(tc);

        assertNotNull(tc.getId());
        assertTrue(tc.isMandatory());
    }

    @Test
    void courseCodeUniqueness() {
        TrainingCourse tc1 = new TrainingCourse();
        tc1.setCode("DUP");
        tc1.setName("Course 1");
        courseRepo.save(tc1);

        TrainingCourse tc2 = new TrainingCourse();
        tc2.setCode("DUP");
        tc2.setName("Course 2");
        assertThrows(DataIntegrityViolationException.class, () -> {
            courseRepo.saveAndFlush(tc2);
        });
    }

    @Test
    void semesterCreation() {
        Semester sem = new Semester();
        sem.setCode("2026-S1");
        sem.setName("Spring 2026");
        sem.setStartDate(LocalDate.of(2026, 1, 15));
        sem.setEndDate(LocalDate.of(2026, 5, 30));
        sem = masterDataService.saveSemester(sem);

        assertNotNull(sem.getId());
        assertEquals(LocalDate.of(2026, 1, 15), sem.getStartDate());
    }

    @Test
    void semesterCodeUniqueness() {
        Semester s1 = new Semester();
        s1.setCode("UNIQ-SEM");
        s1.setName("Sem 1");
        s1.setStartDate(LocalDate.of(2026, 1, 1));
        s1.setEndDate(LocalDate.of(2026, 6, 30));
        semesterRepo.save(s1);

        Semester s2 = new Semester();
        s2.setCode("UNIQ-SEM");
        s2.setName("Sem 2");
        s2.setStartDate(LocalDate.of(2026, 7, 1));
        s2.setEndDate(LocalDate.of(2026, 12, 31));
        assertThrows(DataIntegrityViolationException.class, () -> {
            semesterRepo.saveAndFlush(s2);
        });
    }
}
