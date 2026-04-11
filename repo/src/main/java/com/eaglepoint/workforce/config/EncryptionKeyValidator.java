package com.eaglepoint.workforce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class EncryptionKeyValidator {

    private static final Logger log = LoggerFactory.getLogger(EncryptionKeyValidator.class);

    @Value("${app.encryption.key}")
    private String encryptionKey;

    @PostConstruct
    public void validateEncryptionKey() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException(
                    "FATAL: app.encryption.key (APP_ENCRYPTION_KEY) is not set. "
                    + "Biometric data cannot be encrypted without a valid AES-256 key.");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        "FATAL: Encryption key must be 32 bytes (AES-256), got " + keyBytes.length);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("FATAL: Encryption key is not valid base64", e);
        }
        log.info("Encryption key validated: AES-256 ({} bytes)", 32);
    }
}
