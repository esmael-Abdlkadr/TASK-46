package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.CollectorProfile;
import com.eaglepoint.workforce.entity.WorkShift;
import com.eaglepoint.workforce.enums.CollectorStatus;
import com.eaglepoint.workforce.enums.DayOfWeekEnum;
import com.eaglepoint.workforce.repository.CollectorProfileRepository;
import com.eaglepoint.workforce.repository.SiteProfileRepository;
import com.eaglepoint.workforce.repository.WorkShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class CollectorService {

    private final CollectorProfileRepository collectorRepository;
    private final WorkShiftRepository shiftRepository;
    private final SiteProfileRepository siteRepository;

    public CollectorService(CollectorProfileRepository collectorRepository,
                            WorkShiftRepository shiftRepository,
                            SiteProfileRepository siteRepository) {
        this.collectorRepository = collectorRepository;
        this.shiftRepository = shiftRepository;
        this.siteRepository = siteRepository;
    }

    @Transactional(readOnly = true)
    public List<CollectorProfile> findAll() {
        return collectorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CollectorProfile> findById(Long id) {
        return collectorRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CollectorProfile> findByIdWithShifts(Long id) {
        return collectorRepository.findByIdWithShifts(id);
    }

    @Transactional(readOnly = true)
    public List<CollectorProfile> findByStatus(CollectorStatus status) {
        return collectorRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<CollectorProfile> findAvailable() {
        return collectorRepository.findAllAvailable();
    }

    @Transactional(readOnly = true)
    public List<CollectorProfile> findAvailableByZone(String zone) {
        return collectorRepository.findAvailableByZone(zone);
    }

    @Transactional
    public CollectorProfile save(CollectorProfile collector) {
        return collectorRepository.save(collector);
    }

    @Transactional
    public WorkShift addShift(Long collectorId, DayOfWeekEnum day,
                               LocalTime startTime, LocalTime endTime, Long siteId) {
        CollectorProfile collector = collectorRepository.findById(collectorId)
                .orElseThrow(() -> new RuntimeException("Collector not found: " + collectorId));

        validateShiftTimes(startTime, endTime);

        WorkShift shift = new WorkShift();
        shift.setCollector(collector);
        shift.setDayOfWeek(day);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);

        if (siteId != null) {
            siteRepository.findById(siteId).ifPresent(shift::setSite);
        }

        return shiftRepository.save(shift);
    }

    @Transactional
    public void removeShift(Long shiftId) {
        shiftRepository.deleteById(shiftId);
    }

    @Transactional(readOnly = true)
    public List<WorkShift> getShiftsForCollector(Long collectorId) {
        return shiftRepository.findByCollectorId(collectorId);
    }

    @Transactional(readOnly = true)
    public List<WorkShift> getActiveShiftsNow(DayOfWeekEnum day, LocalTime time) {
        return shiftRepository.findActiveShiftsAt(day, time);
    }

    @Transactional(readOnly = true)
    public long countByStatus(CollectorStatus status) {
        return collectorRepository.countByStatus(status);
    }

    private void validateShiftTimes(LocalTime startTime, LocalTime endTime) {
        if (startTime.getMinute() % 15 != 0) {
            throw new IllegalArgumentException("Start time must be in 15-minute increments");
        }
        if (endTime.getMinute() % 15 != 0) {
            throw new IllegalArgumentException("End time must be in 15-minute increments");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }
}
