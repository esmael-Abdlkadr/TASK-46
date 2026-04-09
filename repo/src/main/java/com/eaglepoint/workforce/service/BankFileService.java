package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.BankEntry;
import com.eaglepoint.workforce.entity.BankFileImport;
import com.eaglepoint.workforce.repository.BankEntryRepository;
import com.eaglepoint.workforce.repository.BankFileImportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BankFileService {

    private final BankFileImportRepository importRepo;
    private final BankEntryRepository entryRepo;

    public BankFileService(BankFileImportRepository importRepo, BankEntryRepository entryRepo) {
        this.importRepo = importRepo;
        this.entryRepo = entryRepo;
    }

    @Transactional
    public BankFileImport importBankFile(MultipartFile file, Long importedBy,
                                          String importedByUsername) throws Exception {
        byte[] fileBytes = file.getBytes();
        String fileHash = computeSha256(fileBytes);

        if (importRepo.existsByFileHash(fileHash)) {
            throw new RuntimeException("This bank file has already been imported (duplicate detected by SHA-256)");
        }

        List<String[]> rows = parseCsv(new String(fileBytes));
        if (rows.size() < 2) {
            throw new RuntimeException("Bank file has no data rows");
        }

        BankFileImport bfi = new BankFileImport();
        bfi.setFileName(file.getOriginalFilename());
        bfi.setFileHash(fileHash);
        bfi.setTotalEntries(rows.size() - 1);
        bfi.setImportedBy(importedBy);
        bfi.setImportedByUsername(importedByUsername);
        bfi = importRepo.save(bfi);

        String[] header = rows.get(0);
        int refIdx = findColumnIndex(header, "Reference", "Ref", "BankReference");
        int amtIdx = findColumnIndex(header, "Amount");
        int dateIdx = findColumnIndex(header, "Date", "EntryDate");
        int descIdx = findColumnIndex(header, "Description", "Desc");

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            BankEntry entry = new BankEntry();
            entry.setBankFileImport(bfi);
            entry.setBankReference(getField(row, refIdx));
            entry.setAmount(new BigDecimal(getField(row, amtIdx).replace(",", "")));
            entry.setEntryDate(parseDate(getField(row, dateIdx)));
            entry.setDescription(descIdx >= 0 ? getField(row, descIdx) : "");
            entryRepo.save(entry);
        }

        return bfi;
    }

    @Transactional(readOnly = true)
    public List<BankFileImport> findAllImports() {
        return importRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<BankFileImport> findImportById(Long id) {
        return importRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<BankEntry> findEntriesByImport(Long importId) {
        return entryRepo.findByBankFileImportId(importId);
    }

    @Transactional(readOnly = true)
    public List<BankEntry> findUnmatchedEntries() {
        return entryRepo.findByMatchedFalse();
    }

    private String computeSha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private List<String[]> parseCsv(String content) {
        List<String[]> rows = new ArrayList<>();
        for (String line : content.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            rows.add(line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1));
        }
        return rows;
    }

    private int findColumnIndex(String[] header, String... names) {
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim().replace("\"", "");
            for (String name : names) {
                if (h.equalsIgnoreCase(name)) return i;
            }
        }
        if (names.length > 0 && (names[0].equals("Reference") || names[0].equals("Amount") || names[0].equals("Date"))) {
            throw new RuntimeException("Required column '" + names[0] + "' not found in bank file. "
                    + "Expected columns: Reference, Amount, Date, Description (optional)");
        }
        return -1;
    }

    private String getField(String[] row, int idx) {
        if (idx < 0 || idx >= row.length) return "";
        return row[idx].trim().replace("\"", "");
    }

    private LocalDate parseDate(String dateStr) {
        for (String pattern : new String[]{"yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy"}) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {}
        }
        return LocalDate.now();
    }
}
