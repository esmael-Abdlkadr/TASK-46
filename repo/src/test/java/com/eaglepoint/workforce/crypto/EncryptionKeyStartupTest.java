package com.eaglepoint.workforce.crypto;

import com.eaglepoint.workforce.config.EncryptionKeyValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests EncryptionKeyValidator fail-fast behavior:
 * - Missing key => IllegalStateException
 * - Blank key => IllegalStateException
 * - Invalid base64 => IllegalStateException
 * - Wrong key length => IllegalStateException
 * - Valid 32-byte key => success
 */
class EncryptionKeyStartupTest {

    @Test
    void missingKey_throwsIllegalState() {
        EncryptionKeyValidator validator = new EncryptionKeyValidator();
        ReflectionTestUtils.setField(validator, "encryptionKey", null);
        assertThrows(IllegalStateException.class, validator::validateEncryptionKey);
    }

    @Test
    void blankKey_throwsIllegalState() {
        EncryptionKeyValidator validator = new EncryptionKeyValidator();
        ReflectionTestUtils.setField(validator, "encryptionKey", "   ");
        assertThrows(IllegalStateException.class, validator::validateEncryptionKey);
    }

    @Test
    void invalidBase64_throwsIllegalState() {
        EncryptionKeyValidator validator = new EncryptionKeyValidator();
        ReflectionTestUtils.setField(validator, "encryptionKey", "not!!valid@@base64");
        assertThrows(IllegalStateException.class, validator::validateEncryptionKey);
    }

    @Test
    void wrongKeyLength_throwsIllegalState() {
        EncryptionKeyValidator validator = new EncryptionKeyValidator();
        // "short" = 5 bytes, not 32
        ReflectionTestUtils.setField(validator, "encryptionKey", "c2hvcnQ=");
        assertThrows(IllegalStateException.class, validator::validateEncryptionKey);
    }

    @Test
    void validKey_succeeds() {
        EncryptionKeyValidator validator = new EncryptionKeyValidator();
        // Valid 32-byte key
        ReflectionTestUtils.setField(validator, "encryptionKey",
                "dGhpcy1pcy1hLTMyLWJ5dGUta2V5ISEhMTIzNDU2Nzg=");
        assertDoesNotThrow(validator::validateEncryptionKey);
    }
}
