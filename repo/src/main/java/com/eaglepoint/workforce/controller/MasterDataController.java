package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.MasterDataService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/masterdata")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    // === Departments ===
    @GetMapping("/departments")
    @Audited(action = AuditAction.READ, resource = "DepartmentList")
    public String departments(Model model) {
        model.addAttribute("departments", masterDataService.findAllDepartments());
        return "masterdata/departments";
    }

    @PostMapping("/departments/save")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Audited(action = AuditAction.CREATE, resource = "Department")
    public String saveDepartment(@RequestParam String code, @RequestParam String name,
                                  @RequestParam(required = false) String headName,
                                  @RequestParam(required = false) Long id,
                                  RedirectAttributes redirectAttrs) {
        Department dept;
        if (id != null) {
            dept = masterDataService.findDepartmentById(id)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
        } else {
            dept = new Department();
        }
        dept.setCode(code);
        dept.setName(name);
        dept.setHeadName(headName);
        masterDataService.saveDepartment(dept);
        redirectAttrs.addFlashAttribute("success", "Department saved");
        return "redirect:/masterdata/departments";
    }

    // === Staffing Classes ===
    @GetMapping("/classes")
    @Audited(action = AuditAction.READ, resource = "StaffingClassList")
    public String classes(Model model) {
        model.addAttribute("classes", masterDataService.findAllClasses());
        model.addAttribute("departments", masterDataService.findAllDepartments());
        return "masterdata/classes";
    }

    @PostMapping("/classes/save")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Audited(action = AuditAction.CREATE, resource = "StaffingClass")
    public String saveClass(@RequestParam String code, @RequestParam String name,
                             @RequestParam(required = false) Long departmentId,
                             @RequestParam(required = false) Integer maxHeadcount,
                             @RequestParam(required = false) Long id,
                             RedirectAttributes redirectAttrs) {
        StaffingClass sc;
        if (id != null) {
            sc = masterDataService.findClassById(id)
                    .orElseThrow(() -> new RuntimeException("Class not found"));
        } else {
            sc = new StaffingClass();
        }
        sc.setCode(code);
        sc.setName(name);
        sc.setMaxHeadcount(maxHeadcount);
        if (departmentId != null) {
            masterDataService.findDepartmentById(departmentId).ifPresent(sc::setDepartment);
        }
        masterDataService.saveClass(sc);
        redirectAttrs.addFlashAttribute("success", "Staffing class saved");
        return "redirect:/masterdata/classes";
    }

    // === Training Courses ===
    @GetMapping("/courses")
    @Audited(action = AuditAction.READ, resource = "TrainingCourseList")
    public String courses(Model model) {
        model.addAttribute("courses", masterDataService.findAllCourses());
        model.addAttribute("departments", masterDataService.findAllDepartments());
        return "masterdata/courses";
    }

    @PostMapping("/courses/save")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Audited(action = AuditAction.CREATE, resource = "TrainingCourse")
    public String saveCourse(@RequestParam String code, @RequestParam String name,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) Long departmentId,
                              @RequestParam(required = false) Integer creditHours,
                              @RequestParam(required = false) boolean mandatory,
                              @RequestParam(required = false) Long id,
                              RedirectAttributes redirectAttrs) {
        TrainingCourse tc;
        if (id != null) {
            tc = masterDataService.findCourseById(id)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
        } else {
            tc = new TrainingCourse();
        }
        tc.setCode(code);
        tc.setName(name);
        tc.setDescription(description);
        tc.setCreditHours(creditHours);
        tc.setMandatory(mandatory);
        if (departmentId != null) {
            masterDataService.findDepartmentById(departmentId).ifPresent(tc::setDepartment);
        }
        masterDataService.saveCourse(tc);
        redirectAttrs.addFlashAttribute("success", "Training course saved");
        return "redirect:/masterdata/courses";
    }

    // === Semesters ===
    @GetMapping("/semesters")
    @Audited(action = AuditAction.READ, resource = "SemesterList")
    public String semesters(Model model) {
        model.addAttribute("semesters", masterDataService.findAllSemesters());
        return "masterdata/semesters";
    }

    @PostMapping("/semesters/save")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Audited(action = AuditAction.CREATE, resource = "Semester")
    public String saveSemester(@RequestParam String code, @RequestParam String name,
                                @RequestParam String startDate, @RequestParam String endDate,
                                @RequestParam(required = false) Long id,
                                RedirectAttributes redirectAttrs) {
        Semester sem;
        if (id != null) {
            sem = masterDataService.findSemesterById(id)
                    .orElseThrow(() -> new RuntimeException("Semester not found"));
        } else {
            sem = new Semester();
        }
        sem.setCode(code);
        sem.setName(name);
        sem.setStartDate(LocalDate.parse(startDate));
        sem.setEndDate(LocalDate.parse(endDate));
        masterDataService.saveSemester(sem);
        redirectAttrs.addFlashAttribute("success", "Semester saved");
        return "redirect:/masterdata/semesters";
    }
}
