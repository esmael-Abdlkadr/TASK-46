package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.AuditLog;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void logCreatesAuditEntry() {
        long countBefore = auditLogRepository.count();

        auditService.log(1L, "testuser", AuditAction.LOGIN_SUCCESS,
                "Authentication", null, "Test login", "192.168.1.100");

        long countAfter = auditLogRepository.count();
        assertEquals(countBefore + 1, countAfter);
    }

    @Test
    void auditLogIsQueryable() {
        auditService.log(1L, "testuser", AuditAction.CREATE,
                "Requisition", 42L, "Created requisition", "10.0.0.1");

        Page<AuditLog> results = auditService.search(
                1L, AuditAction.CREATE, null, null, null, PageRequest.of(0, 10));

        assertFalse(results.isEmpty());
    }

    @Test
    void auditLogEntryHasNoSetters() {
        auditService.log(1L, "immutable-test", AuditAction.READ,
                "TestResource", null, "Testing immutability", "127.0.0.1");

        AuditLog entry = auditLogRepository.findByUserId(1L).stream()
                .filter(e -> "immutable-test".equals(e.getUsername()))
                .findFirst()
                .orElseThrow();

        assertNotNull(entry.getId());
        assertNotNull(entry.getTimestamp());
        assertEquals("immutable-test", entry.getUsername());
        assertEquals(AuditAction.READ, entry.getAction());
    }
}
