package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.crypto.BiometricAttributeConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_templates")
public class BiometricTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Lob
    @Column(name = "template_data", nullable = false, columnDefinition = "LONGBLOB")
    @Convert(converter = BiometricAttributeConverter.class)
    private byte[] templateData;

    @Column(name = "template_hash", length = 64)
    private String templateHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public byte[] getTemplateData() { return templateData; }
    public void setTemplateData(byte[] templateData) { this.templateData = templateData; }
    public String getTemplateHash() { return templateHash; }
    public void setTemplateHash(String templateHash) { this.templateHash = templateHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
