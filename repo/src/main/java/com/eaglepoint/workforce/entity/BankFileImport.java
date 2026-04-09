package com.eaglepoint.workforce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_file_imports", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bank_file_hash", columnNames = "file_hash")
})
public class BankFileImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "file_hash", nullable = false, unique = true, length = 64)
    private String fileHash;

    @Column(name = "total_entries")
    private Integer totalEntries;

    @Column(name = "imported_by", nullable = false)
    private Long importedBy;

    @Column(name = "imported_by_username", length = 50)
    private String importedByUsername;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public Integer getTotalEntries() { return totalEntries; }
    public void setTotalEntries(Integer totalEntries) { this.totalEntries = totalEntries; }
    public Long getImportedBy() { return importedBy; }
    public void setImportedBy(Long importedBy) { this.importedBy = importedBy; }
    public String getImportedByUsername() { return importedByUsername; }
    public void setImportedByUsername(String importedByUsername) { this.importedByUsername = importedByUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
