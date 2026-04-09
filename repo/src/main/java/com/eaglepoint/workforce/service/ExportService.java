package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.ExportStatus;
import com.eaglepoint.workforce.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ExportService {

    @Value("${app.export.storage-path:./exports}")
    private String storagePath;

    private final ExportJobRepository exportJobRepo;
    private final CandidateProfileRepository candidateRepo;
    private final CollectorProfileRepository collectorRepo;
    private final DepartmentRepository departmentRepo;
    private final TrainingCourseRepository courseRepo;

    public ExportService(ExportJobRepository exportJobRepo,
                          CandidateProfileRepository candidateRepo,
                          CollectorProfileRepository collectorRepo,
                          DepartmentRepository departmentRepo,
                          TrainingCourseRepository courseRepo) {
        this.exportJobRepo = exportJobRepo;
        this.candidateRepo = candidateRepo;
        this.collectorRepo = collectorRepo;
        this.departmentRepo = departmentRepo;
        this.courseRepo = courseRepo;
    }

    @Transactional
    public ExportJob queueExport(String name, String exportType, String fileFormat,
                                  String searchCriteriaJson, Long createdBy) {
        ExportJob job = new ExportJob();
        job.setName(name);
        job.setExportType(exportType);
        job.setFileFormat(fileFormat);
        job.setSearchCriteriaJson(searchCriteriaJson);
        job.setCreatedBy(createdBy);
        job.setStatus(ExportStatus.QUEUED);
        return exportJobRepo.save(job);
    }

    @Async("exportExecutor")
    public void executeExport(Long jobId) {
        ExportJob job = exportJobRepo.findById(jobId).orElse(null);
        if (job == null) return;

        job.setStatus(ExportStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        exportJobRepo.save(job);

        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String ext = "csv".equalsIgnoreCase(job.getFileFormat()) ? ".csv" : ".xlsx";
            String fileName = job.getExportType() + "_" + timestamp + ext;
            Path filePath = dir.resolve(fileName);

            int count;
            if ("csv".equalsIgnoreCase(job.getFileFormat())) {
                count = exportToCsv(job.getExportType(), filePath);
            } else {
                count = exportToExcel(job.getExportType(), filePath);
            }

            job.setStatus(ExportStatus.COMPLETED);
            job.setFilePath(filePath.toString());
            job.setFileName(fileName);
            job.setRecordCount(count);
            job.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            job.setStatus(ExportStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        }
        exportJobRepo.save(job);
    }

    @Transactional(readOnly = true)
    public List<ExportJob> findAll() {
        return exportJobRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ExportJob> findByUser(Long userId) {
        return exportJobRepo.findByCreatedByOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<ExportJob> findById(Long id) {
        return exportJobRepo.findById(id);
    }

    public byte[] getFileBytes(ExportJob job) throws IOException {
        return Files.readAllBytes(Paths.get(job.getFilePath()));
    }

    private int exportToExcel(String type, Path filePath) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Export");
            int rowNum = 0;

            switch (type) {
                case "candidates" -> rowNum = writeCandidateRows(sheet);
                case "collectors" -> rowNum = writeCollectorRows(sheet);
                case "departments" -> rowNum = writeDepartmentRows(sheet);
                case "courses" -> rowNum = writeCourseRows(sheet);
                default -> throw new RuntimeException("Unknown export type: " + type);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                wb.write(fos);
            }
            return rowNum;
        }
    }

    private int exportToCsv(String type, Path filePath) throws IOException {
        List<String[]> rows;
        switch (type) {
            case "candidates" -> rows = getCandidateCsvRows();
            case "collectors" -> rows = getCollectorCsvRows();
            case "departments" -> rows = getDepartmentCsvRows();
            case "courses" -> rows = getCourseCsvRows();
            default -> throw new RuntimeException("Unknown export type: " + type);
        }

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(filePath))) {
            for (String[] row : rows) {
                pw.println(String.join(",", escapeCsvFields(row)));
            }
        }
        return rows.size() - 1; // exclude header
    }

    private int writeCandidateRows(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] cols = {"ID", "First Name", "Last Name", "Email", "Phone", "Location",
                "Title", "Employer", "Years Exp", "Education", "Stage", "Tags"};
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

        List<CandidateProfile> list = candidateRepo.findAll();
        for (int i = 0; i < list.size(); i++) {
            CandidateProfile c = list.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getFirstName());
            row.createCell(2).setCellValue(c.getLastName());
            row.createCell(3).setCellValue(c.getEmail() != null ? c.getEmail() : "");
            row.createCell(4).setCellValue(c.getPhone() != null ? c.getPhone() : "");
            row.createCell(5).setCellValue(c.getLocation() != null ? c.getLocation() : "");
            row.createCell(6).setCellValue(c.getCurrentTitle() != null ? c.getCurrentTitle() : "");
            row.createCell(7).setCellValue(c.getCurrentEmployer() != null ? c.getCurrentEmployer() : "");
            row.createCell(8).setCellValue(c.getYearsOfExperience() != null ? c.getYearsOfExperience() : 0);
            row.createCell(9).setCellValue(c.getEducationLevel() != null ? c.getEducationLevel() : "");
            row.createCell(10).setCellValue(c.getPipelineStage().name());
            row.createCell(11).setCellValue(c.getTags() != null ? c.getTags() : "");
        }
        return list.size();
    }

    private int writeCollectorRows(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] cols = {"ID", "Employee ID", "First Name", "Last Name", "Phone", "Email", "Zone", "Status"};
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

        List<CollectorProfile> list = collectorRepo.findAll();
        for (int i = 0; i < list.size(); i++) {
            CollectorProfile c = list.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getEmployeeId() != null ? c.getEmployeeId() : "");
            row.createCell(2).setCellValue(c.getFirstName());
            row.createCell(3).setCellValue(c.getLastName());
            row.createCell(4).setCellValue(c.getPhone() != null ? c.getPhone() : "");
            row.createCell(5).setCellValue(c.getEmail() != null ? c.getEmail() : "");
            row.createCell(6).setCellValue(c.getZone() != null ? c.getZone() : "");
            row.createCell(7).setCellValue(c.getStatus().name());
        }
        return list.size();
    }

    private int writeDepartmentRows(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] cols = {"ID", "Code", "Name", "Head Name", "Active"};
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

        List<Department> list = departmentRepo.findAll();
        for (int i = 0; i < list.size(); i++) {
            Department d = list.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(d.getId());
            row.createCell(1).setCellValue(d.getCode());
            row.createCell(2).setCellValue(d.getName());
            row.createCell(3).setCellValue(d.getHeadName() != null ? d.getHeadName() : "");
            row.createCell(4).setCellValue(d.isActive() ? "Yes" : "No");
        }
        return list.size();
    }

    private int writeCourseRows(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] cols = {"ID", "Code", "Name", "Description", "Credit Hours", "Mandatory", "Active"};
        for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

        List<TrainingCourse> list = courseRepo.findAll();
        for (int i = 0; i < list.size(); i++) {
            TrainingCourse tc = list.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(tc.getId());
            row.createCell(1).setCellValue(tc.getCode());
            row.createCell(2).setCellValue(tc.getName());
            row.createCell(3).setCellValue(tc.getDescription() != null ? tc.getDescription() : "");
            row.createCell(4).setCellValue(tc.getCreditHours() != null ? tc.getCreditHours() : 0);
            row.createCell(5).setCellValue(tc.isMandatory() ? "Yes" : "No");
            row.createCell(6).setCellValue(tc.isActive() ? "Yes" : "No");
        }
        return list.size();
    }

    private List<String[]> getCandidateCsvRows() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"ID", "First Name", "Last Name", "Email", "Location", "Title", "Years Exp", "Stage"});
        for (CandidateProfile c : candidateRepo.findAll()) {
            rows.add(new String[]{String.valueOf(c.getId()), c.getFirstName(), c.getLastName(),
                    c.getEmail(), c.getLocation(), c.getCurrentTitle(),
                    String.valueOf(c.getYearsOfExperience()), c.getPipelineStage().name()});
        }
        return rows;
    }

    private List<String[]> getCollectorCsvRows() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"ID", "Employee ID", "First Name", "Last Name", "Phone", "Zone", "Status"});
        for (CollectorProfile c : collectorRepo.findAll()) {
            rows.add(new String[]{String.valueOf(c.getId()), c.getEmployeeId(), c.getFirstName(),
                    c.getLastName(), c.getPhone(), c.getZone(), c.getStatus().name()});
        }
        return rows;
    }

    private List<String[]> getDepartmentCsvRows() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"Code", "Name", "Head Name", "Active"});
        for (Department d : departmentRepo.findAll()) {
            rows.add(new String[]{d.getCode(), d.getName(), d.getHeadName(), d.isActive() ? "Yes" : "No"});
        }
        return rows;
    }

    private List<String[]> getCourseCsvRows() {
        List<String[]> rows = new java.util.ArrayList<>();
        rows.add(new String[]{"Code", "Name", "Credit Hours", "Mandatory", "Active"});
        for (TrainingCourse tc : courseRepo.findAll()) {
            rows.add(new String[]{tc.getCode(), tc.getName(), String.valueOf(tc.getCreditHours()),
                    tc.isMandatory() ? "Yes" : "No", tc.isActive() ? "Yes" : "No"});
        }
        return rows;
    }

    private String[] escapeCsvFields(String[] fields) {
        String[] result = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String f = fields[i] != null ? fields[i] : "";
            if (f.contains(",") || f.contains("\"") || f.contains("\n")) {
                result[i] = "\"" + f.replace("\"", "\"\"") + "\"";
            } else {
                result[i] = f;
            }
        }
        return result;
    }
}
