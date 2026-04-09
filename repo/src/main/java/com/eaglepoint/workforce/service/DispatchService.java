package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.CollectorProfile;
import com.eaglepoint.workforce.entity.DispatchAssignment;
import com.eaglepoint.workforce.entity.DispatchOfferLog;
import com.eaglepoint.workforce.entity.SiteProfile;
import com.eaglepoint.workforce.enums.CollectorStatus;
import com.eaglepoint.workforce.enums.DispatchMode;
import com.eaglepoint.workforce.enums.DispatchStatus;
import com.eaglepoint.workforce.repository.CollectorProfileRepository;
import com.eaglepoint.workforce.repository.DispatchAssignmentRepository;
import com.eaglepoint.workforce.repository.DispatchOfferLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DispatchService {

    private static final int ACCEPTANCE_TIMEOUT_SECONDS = 90;

    private final DispatchAssignmentRepository assignmentRepository;
    private final CollectorProfileRepository collectorRepository;
    private final DispatchOfferLogRepository offerLogRepository;
    private final SiteService siteService;

    public DispatchService(DispatchAssignmentRepository assignmentRepository,
                           CollectorProfileRepository collectorRepository,
                           DispatchOfferLogRepository offerLogRepository,
                           SiteService siteService) {
        this.assignmentRepository = assignmentRepository;
        this.collectorRepository = collectorRepository;
        this.offerLogRepository = offerLogRepository;
        this.siteService = siteService;
    }

    @Transactional
    public DispatchAssignment createAssignment(SiteProfile site, String title, String description,
                                                LocalDateTime scheduledStart, LocalDateTime scheduledEnd,
                                                Long assignedBy) {
        if (site.isAtCapacity()) {
            throw new RuntimeException("Site " + site.getName() + " is at capacity");
        }

        DispatchAssignment assignment = new DispatchAssignment();
        assignment.setSite(site);
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setScheduledStart(scheduledStart);
        assignment.setScheduledEnd(scheduledEnd);
        assignment.setDispatchMode(site.getDispatchMode());
        assignment.setAssignedBy(assignedBy);
        assignment.setStatus(DispatchStatus.PENDING);

        assignment = assignmentRepository.save(assignment);

        if (site.getDispatchMode() == DispatchMode.ASSIGNED_ORDER) {
            autoAssignToNextCollector(assignment);
        }

        return assignment;
    }

    @Transactional
    public DispatchAssignment offerToCollector(Long assignmentId, Long collectorId) {
        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));
        CollectorProfile collector = collectorRepository.findById(collectorId)
                .orElseThrow(() -> new RuntimeException("Collector not found: " + collectorId));

        if (collector.getStatus() != CollectorStatus.AVAILABLE) {
            throw new RuntimeException("Collector " + collector.getFullName() + " is not available");
        }

        long activeCount = assignmentRepository.countActiveByCollectorId(collectorId);
        if (activeCount >= collector.getMaxConcurrentJobs()) {
            throw new RuntimeException("Collector " + collector.getFullName() +
                    " has reached max concurrent jobs (" + collector.getMaxConcurrentJobs() + ")");
        }

        assignment.setCollector(collector);
        assignment.setStatus(DispatchStatus.OFFERED);
        assignment.setAcceptanceExpiresAt(LocalDateTime.now().plusSeconds(ACCEPTANCE_TIMEOUT_SECONDS));
        assignment.setOfferCount(assignment.getOfferCount() + 1);

        offerLogRepository.save(new DispatchOfferLog(
                assignmentId, collectorId, collector.getFullName(),
                "OFFERED", LocalDateTime.now()));

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public DispatchAssignment acceptAssignment(Long assignmentId, Long collectorId) {
        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        if (assignment.getStatus() != DispatchStatus.OFFERED) {
            throw new RuntimeException("Assignment is not in OFFERED status");
        }
        if (assignment.getCollector() == null || !assignment.getCollector().getId().equals(collectorId)) {
            throw new RuntimeException("Assignment is not offered to this collector");
        }
        if (assignment.isAcceptanceExpired()) {
            throw new RuntimeException("Acceptance window has expired");
        }

        assignment.setStatus(DispatchStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());
        assignment.setAcceptanceExpiresAt(null);

        siteService.incrementOccupancy(assignment.getSite().getId());

        DispatchOfferLog log = offerLogRepository.findByAssignmentIdOrderByOfferedAtDesc(assignmentId)
                .stream().findFirst().orElse(null);
        if (log != null) {
            log.setOutcome("ACCEPTED");
            log.setRespondedAt(LocalDateTime.now());
            offerLogRepository.save(log);
        }

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public DispatchAssignment declineAssignment(Long assignmentId, Long collectorId) {
        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        if (assignment.getCollector() == null || !assignment.getCollector().getId().equals(collectorId)) {
            throw new RuntimeException("Assignment is not offered to this collector");
        }

        DispatchOfferLog log = offerLogRepository.findByAssignmentIdOrderByOfferedAtDesc(assignmentId)
                .stream().findFirst().orElse(null);
        if (log != null) {
            log.setOutcome("DECLINED");
            log.setRespondedAt(LocalDateTime.now());
            offerLogRepository.save(log);
        }

        assignment.setCollector(null);
        assignment.setStatus(DispatchStatus.PENDING);
        assignment.setAcceptanceExpiresAt(null);
        assignment = assignmentRepository.save(assignment);

        autoAssignToNextCollector(assignment);

        return assignment;
    }

    @Transactional
    public DispatchAssignment grabJob(Long assignmentId, Long collectorId) {
        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        if (assignment.getDispatchMode() != DispatchMode.GRAB_ORDER) {
            throw new RuntimeException("This assignment is not in grab-order mode");
        }
        if (assignment.getStatus() != DispatchStatus.PENDING) {
            throw new RuntimeException("This job is no longer available for grabbing");
        }

        CollectorProfile collector = collectorRepository.findById(collectorId)
                .orElseThrow(() -> new RuntimeException("Collector not found: " + collectorId));

        if (assignment.getSite().isAtCapacity()) {
            throw new RuntimeException("Site is at capacity");
        }

        assignment.setCollector(collector);
        assignment.setStatus(DispatchStatus.OFFERED);
        assignment.setAcceptanceExpiresAt(LocalDateTime.now().plusSeconds(ACCEPTANCE_TIMEOUT_SECONDS));
        assignment.setOfferCount(assignment.getOfferCount() + 1);

        offerLogRepository.save(new DispatchOfferLog(
                assignmentId, collectorId, collector.getFullName(),
                "GRABBED", LocalDateTime.now()));

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public DispatchAssignment completeAssignment(Long assignmentId) {
        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        assignment.setStatus(DispatchStatus.COMPLETED);
        assignment.setCompletedAt(LocalDateTime.now());

        siteService.decrementOccupancy(assignment.getSite().getId());

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public DispatchAssignment cancelAssignment(Long assignmentId) {
        DispatchAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        if (assignment.getStatus() == DispatchStatus.ACCEPTED ||
            assignment.getStatus() == DispatchStatus.IN_PROGRESS) {
            siteService.decrementOccupancy(assignment.getSite().getId());
        }

        assignment.setStatus(DispatchStatus.CANCELLED);
        assignment.setAcceptanceExpiresAt(null);

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public int processExpiredOffers() {
        List<DispatchAssignment> expired = assignmentRepository.findExpiredOffers(LocalDateTime.now());
        int count = 0;
        for (DispatchAssignment assignment : expired) {
            if (assignment.getCollector() != null) {
                offerLogRepository.save(new DispatchOfferLog(
                        assignment.getId(), assignment.getCollector().getId(),
                        assignment.getCollector().getFullName(),
                        "TIMED_OUT", assignment.getAcceptanceExpiresAt()));
            }

            assignment.setCollector(null);
            assignment.setStatus(DispatchStatus.PENDING);
            assignment.setAcceptanceExpiresAt(null);
            assignmentRepository.save(assignment);

            autoAssignToNextCollector(assignment);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<DispatchAssignment> findAllWithDetails() {
        return assignmentRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public List<DispatchAssignment> findByStatusWithDetails(List<DispatchStatus> statuses) {
        return assignmentRepository.findByStatusInWithDetails(statuses);
    }

    @Transactional(readOnly = true)
    public Optional<DispatchAssignment> findById(Long id) {
        return assignmentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<DispatchAssignment> findGrabbableJobs() {
        return assignmentRepository.findByStatus(DispatchStatus.PENDING).stream()
                .filter(a -> a.getDispatchMode() == DispatchMode.GRAB_ORDER)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DispatchOfferLog> getOfferHistory(Long assignmentId) {
        return offerLogRepository.findByAssignmentIdOrderByOfferedAtDesc(assignmentId);
    }

    @Transactional(readOnly = true)
    public long countByStatus(DispatchStatus status) {
        return assignmentRepository.countByStatus(status);
    }

    private void autoAssignToNextCollector(DispatchAssignment assignment) {
        if (assignment.getDispatchMode() != DispatchMode.ASSIGNED_ORDER) {
            return;
        }

        String zone = assignment.getSite().getZone();
        List<CollectorProfile> available;
        if (zone != null && !zone.isBlank()) {
            available = collectorRepository.findAvailableByZone(zone);
        } else {
            available = collectorRepository.findAllAvailable();
        }

        List<Long> previouslyOfferedIds = offerLogRepository
                .findByAssignmentIdOrderByOfferedAtDesc(assignment.getId())
                .stream()
                .map(DispatchOfferLog::getCollectorId)
                .toList();

        for (CollectorProfile collector : available) {
            if (previouslyOfferedIds.contains(collector.getId())) {
                continue;
            }
            long activeCount = assignmentRepository.countActiveByCollectorId(collector.getId());
            if (activeCount >= collector.getMaxConcurrentJobs()) {
                continue;
            }

            assignment.setCollector(collector);
            assignment.setStatus(DispatchStatus.OFFERED);
            assignment.setAcceptanceExpiresAt(LocalDateTime.now().plusSeconds(ACCEPTANCE_TIMEOUT_SECONDS));
            assignment.setOfferCount(assignment.getOfferCount() + 1);
            assignmentRepository.save(assignment);

            offerLogRepository.save(new DispatchOfferLog(
                    assignment.getId(), collector.getId(), collector.getFullName(),
                    "OFFERED", LocalDateTime.now()));
            return;
        }
    }
}
