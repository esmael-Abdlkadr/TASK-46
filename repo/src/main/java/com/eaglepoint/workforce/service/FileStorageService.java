package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.StoredFile;
import com.eaglepoint.workforce.repository.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
public class FileStorageService {

    @Value("${app.file-storage.base-path:./file-store}")
    private String basePath;

    private final StoredFileRepository storedFileRepository;

    public FileStorageService(StoredFileRepository storedFileRepository) {
        this.storedFileRepository = storedFileRepository;
    }

    @Transactional
    public StoredFile store(MultipartFile file, String category, Long uploadedBy) throws IOException {
        byte[] bytes = file.getBytes();
        String fingerprint = computeSha256(bytes);

        Optional<StoredFile> existing = storedFileRepository.findByFingerprint(fingerprint);
        if (existing.isPresent()) {
            return existing.get();
        }

        Path dir = Paths.get(basePath, category != null ? category : "general");
        Files.createDirectories(dir);

        String storedName = System.currentTimeMillis() + "_" + sanitizeFileName(file.getOriginalFilename());
        Path filePath = dir.resolve(storedName);
        Files.write(filePath, bytes);

        StoredFile sf = new StoredFile();
        sf.setOriginalName(file.getOriginalFilename());
        sf.setStoredPath(filePath.toString());
        sf.setFingerprint(fingerprint);
        sf.setFileSize((long) bytes.length);
        sf.setContentType(file.getContentType());
        sf.setCategory(category);
        sf.setUploadedBy(uploadedBy);
        return storedFileRepository.save(sf);
    }

    @Transactional(readOnly = true)
    public Optional<StoredFile> findByFingerprint(String fingerprint) {
        return storedFileRepository.findByFingerprint(fingerprint);
    }

    @Transactional(readOnly = true)
    public boolean existsByFingerprint(String fingerprint) {
        return storedFileRepository.existsByFingerprint(fingerprint);
    }

    @Transactional(readOnly = true)
    public List<StoredFile> findByCategory(String category) {
        return storedFileRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<StoredFile> findAll() {
        return storedFileRepository.findAllByOrderByCreatedAtDesc();
    }

    public byte[] readFile(StoredFile storedFile) throws IOException {
        return Files.readAllBytes(Paths.get(storedFile.getStoredPath()));
    }

    public boolean verifyFingerprint(StoredFile storedFile) {
        try {
            byte[] bytes = readFile(storedFile);
            String currentHash = computeSha256(bytes);
            return currentHash.equals(storedFile.getFingerprint());
        } catch (IOException e) {
            return false;
        }
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 computation failed", e);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unnamed";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
