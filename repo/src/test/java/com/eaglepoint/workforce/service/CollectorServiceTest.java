package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.CollectorProfile;
import com.eaglepoint.workforce.entity.SiteProfile;
import com.eaglepoint.workforce.entity.WorkShift;
import com.eaglepoint.workforce.enums.CollectorStatus;
import com.eaglepoint.workforce.enums.DayOfWeekEnum;
import com.eaglepoint.workforce.enums.DispatchMode;
import com.eaglepoint.workforce.repository.CollectorProfileRepository;
import com.eaglepoint.workforce.repository.SiteProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CollectorServiceTest {

    @Autowired
    private CollectorService collectorService;

    @Autowired
    private CollectorProfileRepository collectorRepository;

    @Autowired
    private SiteProfileRepository siteRepository;

    private CollectorProfile collector;
    private SiteProfile site;

    @BeforeEach
    void setUp() {
        collector = new CollectorProfile();
        collector.setFirstName("Test");
        collector.setLastName("Collector");
        collector.setZone("East");
        collector.setStatus(CollectorStatus.AVAILABLE);
        collector = collectorRepository.save(collector);

        site = new SiteProfile();
        site.setName("Test Site");
        site.setDispatchMode(DispatchMode.ASSIGNED_ORDER);
        site = siteRepository.save(site);
    }

    @Test
    void addShift_validatesIncrements() {
        WorkShift shift = collectorService.addShift(
                collector.getId(), DayOfWeekEnum.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(16, 30), site.getId());

        assertNotNull(shift.getId());
        assertEquals(DayOfWeekEnum.MONDAY, shift.getDayOfWeek());
        assertEquals(LocalTime.of(8, 0), shift.getStartTime());
        assertEquals(LocalTime.of(16, 30), shift.getEndTime());
    }

    @Test
    void addShift_rejectsNon15MinuteIncrements() {
        assertThrows(IllegalArgumentException.class, () ->
                collectorService.addShift(collector.getId(), DayOfWeekEnum.TUESDAY,
                        LocalTime.of(8, 7), LocalTime.of(16, 0), null));
    }

    @Test
    void addShift_rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () ->
                collectorService.addShift(collector.getId(), DayOfWeekEnum.WEDNESDAY,
                        LocalTime.of(17, 0), LocalTime.of(9, 0), null));
    }

    @Test
    void findAvailable_returnsOnlyAvailable() {
        CollectorProfile offDuty = new CollectorProfile();
        offDuty.setFirstName("Off");
        offDuty.setLastName("Duty");
        offDuty.setStatus(CollectorStatus.OFF_DUTY);
        collectorRepository.save(offDuty);

        List<CollectorProfile> available = collectorService.findAvailable();
        assertTrue(available.stream().allMatch(c -> c.getStatus() == CollectorStatus.AVAILABLE));
    }

    @Test
    void getShiftsForCollector_returnsList() {
        collectorService.addShift(collector.getId(), DayOfWeekEnum.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        collectorService.addShift(collector.getId(), DayOfWeekEnum.TUESDAY,
                LocalTime.of(9, 0), LocalTime.of(17, 0), null);

        List<WorkShift> shifts = collectorService.getShiftsForCollector(collector.getId());
        assertEquals(2, shifts.size());
    }

    @Test
    void removeShift_deletesShift() {
        WorkShift shift = collectorService.addShift(
                collector.getId(), DayOfWeekEnum.FRIDAY,
                LocalTime.of(8, 0), LocalTime.of(12, 0), null);

        collectorService.removeShift(shift.getId());

        List<WorkShift> remaining = collectorService.getShiftsForCollector(collector.getId());
        assertTrue(remaining.isEmpty());
    }
}
