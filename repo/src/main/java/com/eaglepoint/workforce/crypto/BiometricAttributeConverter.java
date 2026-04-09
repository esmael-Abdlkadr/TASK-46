package com.eaglepoint.workforce.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class BiometricAttributeConverter implements AttributeConverter<byte[], byte[]> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static final int REQUIRED_KEY_BYTES = 32; // AES-256

    private SecretKeySpec getKey() {
        String keyBase64 = System.getProperty("app.encryption.key",
                System.getenv("APP_ENCRYPTION_KEY"));
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException(
                    "Biometric encryption key not configured. "
                    + "Set APP_ENCRYPTION_KEY env var or app.encryption.key system property "
                    + "with a base64-encoded 32-byte AES key.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("APP_ENCRYPTION_KEY is not valid base64", e);
        }
        if (keyBytes.length != REQUIRED_KEY_BYTES) {
            throw new IllegalStateException(
                    "APP_ENCRYPTION_KEY must decode to exactly " + REQUIRED_KEY_BYTES
                    + " bytes (AES-256), got " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public byte[] convertToDatabaseColumn(byte[] attribute) {
        if (attribute == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), spec);
            byte[] cipherText = cipher.doFinal(attribute);

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return buffer.array();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt biometric data", e);
        }
    }

    @Override
    public byte[] convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) return null;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(dbData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec);
            return cipher.doFinal(cipherText);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt biometric data", e);
        }
    }
}
