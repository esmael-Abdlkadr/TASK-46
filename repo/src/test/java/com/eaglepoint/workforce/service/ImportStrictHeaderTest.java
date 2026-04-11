package com.eaglepoint.workforce.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests strict import header validation:
 * - unknown/extra columns are rejected
 * - missing required columns are rejected
 * - unknown import type is rejected
 */
@SpringBootTest
@ActiveProfiles("test")
class ImportStrictHeaderTest {

    @Autowired private ImportService importService;

    @Test
    void unknownColumn_departments_rejected() {
        String[] header = {"Code", "Name", "UnknownColumn"};
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                importService.validateHeaderSchema("departments", header));
        assertTrue(ex.getMessage().contains("Unknown column"),
                "Should report unknown column, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("UnknownColumn"));
    }

    @Test
    void unknownColumn_courses_rejected() {
        String[] header = {"Code", "Name", "BadCol"};
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                importService.validateHeaderSchema("courses", header));
        assertTrue(ex.getMessage().contains("Unknown column"));
        assertTrue(ex.getMessage().contains("BadCol"));
    }

    @Test
    void missingAndUnknown_combined() {
        // Missing "Name", has unknown "Foo"
        String[] header = {"Code", "Foo"};
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                importService.validateHeaderSchema("departments", header));
        // Should fail on missing first
        assertTrue(ex.getMessage().contains("Missing required column"));
    }

    @Test
    void validDepartmentHeaders_accepted() {
        String[] header = {"Code", "Name", "Head Name", "Active"};
        assertDoesNotThrow(() -> importService.validateHeaderSchema("departments", header));
    }

    @Test
    void validCourseHeaders_accepted() {
        String[] header = {"Code", "Name", "Description", "Credit Hours", "Mandatory", "Active"};
        assertDoesNotThrow(() -> importService.validateHeaderSchema("courses", header));
    }

    @Test
    void minimalRequired_accepted() {
        String[] header = {"Code", "Name"};
        assertDoesNotThrow(() -> importService.validateHeaderSchema("departments", header));
    }

    @Test
    void unknownImportType_rejected() {
        String[] header = {"Code", "Name"};
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                importService.validateHeaderSchema("badtype", header));
        assertTrue(ex.getMessage().contains("Unknown import type"));
    }
}
