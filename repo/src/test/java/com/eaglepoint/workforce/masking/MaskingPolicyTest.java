package com.eaglepoint.workforce.masking;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaskingPolicyTest {

    @Test
    void adminSeesFullValue() {
        assertEquals("John Doe", MaskingUtil.mask("John Doe", true));
    }

    @Test
    void nonAdminSeesMasked() {
        assertEquals("**** Doe", MaskingUtil.mask("John Doe", false));
    }

    @Test
    void emailMasking_adminFull() {
        assertEquals("test@example.com", MaskingUtil.maskEmailByRole("test@example.com", true));
    }

    @Test
    void emailMasking_nonAdminMasked() {
        String masked = MaskingUtil.maskEmailByRole("test@example.com", false);
        assertTrue(masked.startsWith("t"));
        assertTrue(masked.contains("@example.com"));
        assertTrue(masked.contains("*"));
    }

    @Test
    void phoneMasking_nonAdmin() {
        String masked = MaskingUtil.maskPhoneByRole("555-123-4567", false);
        assertTrue(masked.endsWith("4567"));
        assertTrue(masked.contains("*"));
    }

    @Test
    void phoneMasking_adminFull() {
        assertEquals("555-123-4567", MaskingUtil.maskPhoneByRole("555-123-4567", true));
    }

    @Test
    void nullHandling() {
        assertNull(MaskingUtil.mask(null, true));
        assertNull(MaskingUtil.mask(null, false));
        assertNull(MaskingUtil.maskEmailByRole(null, false));
        assertNull(MaskingUtil.maskPhoneByRole(null, false));
    }
}
