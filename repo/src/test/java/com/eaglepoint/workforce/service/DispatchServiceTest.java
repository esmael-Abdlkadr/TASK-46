package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.CollectorProfile;
import com.eaglepoint.workforce.entity.DispatchAssignment;
import com.eaglepoint.workforce.entity.SiteProfile;
import com.eaglepoint.workforce.enums.CollectorStatus;
import com.eaglepoint.workforce.enums.DispatchMode;
import com.eaglepoint.workforce.enums.DispatchStatus;
import com.eaglepoint.workforce.repository.CollectorProfileRepository;
import com.eaglepoint.workforce.repository.DispatchAssignmentRepository;
import com.eaglepoint.workforce.repository.SiteProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DispatchServiceTest {

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private SiteProfileRepository siteRepository;

    @Autowired
    private CollectorProfileRepository collectorRepository;

    @Autowired
    private DispatchAssignmentRepository assignmentRepository;

    private SiteProfile assignedSite;
    private SiteProfile grabSite;
    private CollectorProfile collector1;
    private CollectorProfile collector2;

    @BeforeEach
    void setUp() {
        assignedSite = new SiteProfile();
        assignedSite.setName("Test Site A");
        assignedSite.setZone("North");
        assignedSite.setCapacityLimit(5);
        assignedSite.setDispatchMode(DispatchMode.ASSIGNED_ORDER);
        assignedSite = siteRepository.save(assignedSite);

        grabSite = new SiteProfile();
        grabSite.setName("Test Site B");
        grabSite.setZone("North");
        grabSite.setCapacityLimit(3);
        grabSite.setDispatchMode(DispatchMode.GRAB_ORDER);
        grabSite = siteRepository.save(grabSite);

        collector1 = new CollectorProfile();
        collector1.setFirstName("John");
        collector1.setLastName("Collector");
        collector1.setZone("North");
        collector1.setStatus(CollectorStatus.AVAILABLE);
        collector1.setMaxConcurrentJobs(2);
        collector1 = collectorRepository.save(collector1);

        collector2 = new CollectorProfile();
        collector2.setFirstName("Jane");
        collector2.setLastName("Worker");
        collector2.setZone("North");
        collector2.setStatus(CollectorStatus.AVAILABLE);
        collector2 = collectorRepository.save(collector2);
    }

    @Test
    void createAssignmentInAssignedMode_autoDispatches() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                assignedSite, "Test Job", "Description",
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2), 1L);

        assertNotNull(assignment.getId());
        assertEquals(DispatchStatus.OFFERED, assignment.getStatus());
        assertNotNull(assignment.getCollector());
        assertNotNull(assignment.getAcceptanceExpiresAt());
    }

    @Test
    void createAssignmentInGrabMode_staysPending() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                grabSite, "Grab Job", null, null, null, 1L);

        assertEquals(DispatchStatus.PENDING, assignment.getStatus());
        assertNull(assignment.getCollector());
    }

    @Test
    void grabJob_setsOfferedWithTimeout() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                grabSite, "Grab Job", null, null, null, 1L);

        DispatchAssignment grabbed = dispatchService.grabJob(assignment.getId(), collector1.getId());

        assertEquals(DispatchStatus.OFFERED, grabbed.getStatus());
        assertEquals(collector1.getId(), grabbed.getCollector().getId());
        assertNotNull(grabbed.getAcceptanceExpiresAt());
    }

    @Test
    void acceptAssignment_updatesStatusAndOccupancy() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                assignedSite, "Accept Test", null, null, null, 1L);

        DispatchAssignment accepted = dispatchService.acceptAssignment(
                assignment.getId(), assignment.getCollector().getId());

        assertEquals(DispatchStatus.ACCEPTED, accepted.getStatus());
        assertNotNull(accepted.getAcceptedAt());

        SiteProfile updatedSite = siteRepository.findById(assignedSite.getId()).orElseThrow();
        assertEquals(1, updatedSite.getCurrentOccupancy());
    }

    @Test
    void declineAssignment_autoRedispatches() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                assignedSite, "Decline Test", null, null, null, 1L);

        Long firstCollectorId = assignment.getCollector().getId();
        DispatchAssignment declined = dispatchService.declineAssignment(
                assignment.getId(), firstCollectorId);

        // Should be re-offered to another collector
        if (declined.getCollector() != null) {
            assertNotEquals(firstCollectorId, declined.getCollector().getId());
            assertEquals(DispatchStatus.OFFERED, declined.getStatus());
        } else {
            assertEquals(DispatchStatus.PENDING, declined.getStatus());
        }
    }

    @Test
    void completeAssignment_decrementsOccupancy() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                assignedSite, "Complete Test", null, null, null, 1L);
        dispatchService.acceptAssignment(assignment.getId(), assignment.getCollector().getId());

        dispatchService.completeAssignment(assignment.getId());

        SiteProfile updatedSite = siteRepository.findById(assignedSite.getId()).orElseThrow();
        assertEquals(0, updatedSite.getCurrentOccupancy());

        DispatchAssignment completed = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertEquals(DispatchStatus.COMPLETED, completed.getStatus());
    }

    @Test
    void capacityLimitEnforced() {
        grabSite.setCapacityLimit(1);
        grabSite.setCurrentOccupancy(1);
        siteRepository.save(grabSite);

        assertThrows(RuntimeException.class, () ->
                dispatchService.createAssignment(grabSite, "Over Capacity", null, null, null, 1L));
    }

    @Test
    void acceptAfterExpiry_throws() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                assignedSite, "Expiry Test", null, null, null, 1L);

        // Manually expire the window
        assignment.setAcceptanceExpiresAt(LocalDateTime.now().minusSeconds(1));
        assignmentRepository.save(assignment);

        assertThrows(RuntimeException.class, () ->
                dispatchService.acceptAssignment(assignment.getId(), assignment.getCollector().getId()));
    }

    @Test
    void processExpiredOffers_redispatches() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                assignedSite, "Timeout Test", null, null, null, 1L);

        // Manually expire
        assignment.setAcceptanceExpiresAt(LocalDateTime.now().minusSeconds(1));
        assignmentRepository.save(assignment);

        int processed = dispatchService.processExpiredOffers();
        assertEquals(1, processed);
    }

    @Test
    void offerHistory_isTracked() {
        DispatchAssignment assignment = dispatchService.createAssignment(
                assignedSite, "History Test", null, null, null, 1L);

        var history = dispatchService.getOfferHistory(assignment.getId());
        assertFalse(history.isEmpty());
        assertEquals("OFFERED", history.get(0).getOutcome());
    }
}
