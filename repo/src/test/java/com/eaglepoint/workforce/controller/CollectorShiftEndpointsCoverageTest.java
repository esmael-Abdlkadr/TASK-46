package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.entity.CollectorProfile;
import com.eaglepoint.workforce.entity.WorkShift;
import com.eaglepoint.workforce.enums.CollectorStatus;
import com.eaglepoint.workforce.enums.DayOfWeekEnum;
import com.eaglepoint.workforce.repository.CollectorProfileRepository;
import com.eaglepoint.workforce.repository.WorkShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CollectorShiftEndpointsCoverageTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CollectorProfileRepository collectorRepo;
    @Autowired
    private WorkShiftRepository shiftRepo;

    private CollectorProfile collector;

    @BeforeEach
    void setup() {
        collector = new CollectorProfile();
        collector.setFirstName("Shift");
        collector.setLastName("Tester");
        collector.setEmployeeId("COL-SH-" + System.nanoTime());
        collector.setStatus(CollectorStatus.AVAILABLE);
        collector.setMaxConcurrentJobs(3);
        collector = collectorRepo.save(collector);
    }

    @Test
    @WithMockUser(username = "dispatch", roles = "DISPATCH_SUPERVISOR")
    void addShift_thenRemoveShift() throws Exception {
        mockMvc.perform(post("/dispatch/collectors/" + collector.getId() + "/shifts/add").with(csrf())
                        .param("dayOfWeek", DayOfWeekEnum.MONDAY.name())
                        .param("startTime", "09:00")
                        .param("endTime", "17:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/collectors/" + collector.getId()));

        WorkShift shift = shiftRepo.findByCollectorId(collector.getId()).stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/dispatch/collectors/shifts/" + shift.getId() + "/remove").with(csrf())
                        .param("collectorId", collector.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dispatch/collectors/" + collector.getId()));
    }
}
