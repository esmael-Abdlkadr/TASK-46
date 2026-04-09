package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.ImportStatus;
import com.eaglepoint.workforce.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ImportService {

    @Value("${app.import.storage-path:./imports}")
    private String storagePath;

    private final ImportJobRepository importJobRepo;
    private final DepartmentRepository departmentRepo;
    private final TrainingCourseRepository courseRepo;

    public ImportService(ImportJobRepository importJobRepo,
                          DepartmentRepository departmentRepo,
                          TrainingCourseRepository courseRepo) {
        this.importJobRepo = importJobRepo;
        this.departmentRepo = departmentRepo;
        this.courseRepo = courseRepo;
    }

    @Transactional
    public ImportJob prepareImport(MultipartFile file, String importType, Long createdBy) throws Exception {
        String fingerprint = computeSha256(file.getInputStream());

        if (importJobRepo.existsByFileFingerprint(fingerprint)) {
            ImportJob dup = new ImportJob();
            dup.setOriginalFileName(file.getOriginalFilename());
            dup.setImportType(importType);
            dup.setFileFingerprint(fingerprint);
            dup.setStatus(ImportStatus.DUPLICATE);
            dup.setCreatedBy(createdBy);
            dup.setErrorReport("This file has already been imported (SHA-256: " + fingerprint + ")");
            return importJobRepo.save(dup);
        }

        Path dir = Paths.get(storagePath);
        Files.createDirectories(dir);
        String savedName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path savedPath = dir.resolve(savedName);
        file.transferTo(savedPath.toFile());

        ImportJob job = new ImportJob();
        job.setOriginalFileName(file.getOriginalFilename());
        job.setImportType(importType);
        job.setFileFingerprint(fingerprint);
        job.setFilePath(savedPath.toString());
        job.setStatus(ImportStatus.QUEUED);
        job.setCreatedBy(createdBy);
        return importJobRepo.save(job);
    }

    @Async("importExecutor")
    public void executeImport(Long jobId) {
        ImportJob job = importJobRepo.findById(jobId).orElse(null);
        if (job == null || job.getStatus() == ImportStatus.DUPLICATE) return;

        job.setStatus(ImportStatus.VALIDATING);
        importJobRepo.save(job);

        try {
            Path filePath = Paths.get(job.getFilePath());
            String fileName = job.getOriginalFileName().toLowerCase();
            List<String[]> rows;

            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                rows = readExcel(filePath);
            } else if (fileName.endsWith(".csv")) {
                rows = readCsv(filePath);
            } else {
                throw new RuntimeException("Unsupported file format. Use .xlsx, .xls, or .csv");
            }

            if (rows.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            job.setStatus(ImportStatus.IMPORTING);
            job.setTotalRows(rows.size() - 1); // exclude header
            importJobRepo.save(job);

            String[] header = rows.get(0);
            validateHeaderSchema(job.getImportType(), header);

            List<String> errors = new ArrayList<>();
            int successCount = 0;

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                try {
                    validateAndImportRow(job.getImportType(), header, row, i);
                    successCount++;
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }

            job.setSuccessRows(successCount);
            job.setErrorRows(errors.size());
            job.setErrorReport(errors.isEmpty() ? null : String.join("\n", errors));
            job.setStatus(errors.isEmpty() ? ImportStatus.COMPLETED : ImportStatus.COMPLETED_WITH_ERRORS);
            job.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            job.setStatus(ImportStatus.FAILED);
            job.setErrorReport(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        }
        importJobRepo.save(job);
    }

    @Transactional(readOnly = true)
    public List<ImportJob> findAll() {
        return importJobRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ImportJob> findById(Long id) {
        return importJobRepo.findById(id);
    }

    private static final Map<String, Set<String>> REQUIRED_HEADERS = Map.of(
            "departments", Set.of("Code", "Name"),
            "courses", Set.of("Code", "Name")
    );

    void validateHeaderSchema(String importType, String[] header) {
        Set<String> required = REQUIRED_HEADERS.get(importType);
        if (required == null) {
            throw new RuntimeException("Unknown import type: " + importType);
        }
        Set<String> actual = new HashSet<>();
        for (String h : header) actual.add(h.trim());

        List<String> missing = new ArrayList<>();
        for (String r : required) {
            if (!actual.contains(r)) missing.add(r);
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException("Missing required column(s): " + String.join(", ", missing)
                    + ". Expected schema: " + required);
        }
    }

    @Transactional
    void validateAndImportRow(String importType, String[] header, String[] row, int rowIndex) {
        switch (importType) {
            case "departments" -> importDepartmentRow(header, row, rowIndex);
            case "courses" -> importCourseRow(header, row, rowIndex);
            default -> throw new RuntimeException("Unknown import type: " + importType);
        }
    }

    private void importDepartmentRow(String[] header, String[] row, int rowIndex) {
        Map<String, String> map = zipToMap(header, row);
        String code = requireField(map, "Code", rowIndex);
        String name = requireField(map, "Name", rowIndex);

        if (departmentRepo.existsByCode(code)) {
            Department existing = departmentRepo.findByCode(code).orElseThrow();
            existing.setName(name);
            existing.setHeadName(map.getOrDefault("Head Name", existing.getHeadName()));
            departmentRepo.save(existing);
        } else {
            Department dept = new Department();
            dept.setCode(code);
            dept.setName(name);
            dept.setHeadName(map.get("Head Name"));
            dept.setActive(!"No".equalsIgnoreCase(map.get("Active")));
            departmentRepo.save(dept);
        }
    }

    private void importCourseRow(String[] header, String[] row, int rowIndex) {
        Map<String, String> map = zipToMap(header, row);
        String code = requireField(map, "Code", rowIndex);
        String name = requireField(map, "Name", rowIndex);

        if (courseRepo.existsByCode(code)) {
            TrainingCourse existing = courseRepo.findByCode(code).orElseThrow();
            existing.setName(name);
            existing.setDescription(map.getOrDefault("Description", existing.getDescription()));
            if (map.containsKey("Credit Hours")) {
                try { existing.setCreditHours(Integer.parseInt(map.get("Credit Hours"))); } catch (NumberFormatException ignored) {}
            }
            courseRepo.save(existing);
        } else {
            TrainingCourse course = new TrainingCourse();
            course.setCode(code);
            course.setName(name);
            course.setDescription(map.get("Description"));
            if (map.containsKey("Credit Hours")) {
                try { course.setCreditHours(Integer.parseInt(map.get("Credit Hours"))); } catch (NumberFormatException ignored) {}
            }
            course.setMandatory("Yes".equalsIgnoreCase(map.get("Mandatory")));
            course.setActive(!"No".equalsIgnoreCase(map.get("Active")));
            courseRepo.save(course);
        }
    }

    private String requireField(Map<String, String> map, String field, int rowIndex) {
        String val = map.get(field);
        if (val == null || val.isBlank()) {
            throw new RuntimeException("Missing required field '" + field + "'");
        }
        return val.trim();
    }

    private Map<String, String> zipToMap(String[] header, String[] row) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < header.length; i++) {
            map.put(header[i].trim(), i < row.length ? (row[i] != null ? row[i].trim() : "") : "");
        }
        return map;
    }

    private List<String[]> readExcel(Path path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (InputStream is = Files.newInputStream(path); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                String[] cells = new String[row.getLastCellNum()];
                for (int i = 0; i < cells.length; i++) {
                    Cell cell = row.getCell(i);
                    cells[i] = cell != null ? getCellStringValue(cell) : "";
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private String getCellStringValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private List<String[]> readCsv(Path path) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                rows.add(parseCsvLine(line));
            }
        }
        return rows;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    static String computeSha256(InputStream is) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) {
            digest.update(buf, 0, n);
        }
        byte[] hash = digest.digest();
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
