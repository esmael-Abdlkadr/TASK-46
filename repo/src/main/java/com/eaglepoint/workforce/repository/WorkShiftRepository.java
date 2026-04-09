package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.WorkShift;
import com.eaglepoint.workforce.enums.DayOfWeekEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {

    List<WorkShift> findByCollectorId(Long collectorId);

    List<WorkShift> findByDayOfWeek(DayOfWeekEnum dayOfWeek);

    @Query("SELECT ws FROM WorkShift ws WHERE ws.dayOfWeek = :day " +
           "AND ws.startTime <= :time AND ws.endTime > :time")
    List<WorkShift> findActiveShiftsAt(@Param("day") DayOfWeekEnum day, @Param("time") LocalTime time);

    @Query("SELECT ws FROM WorkShift ws WHERE ws.collector.id = :collectorId " +
           "AND ws.dayOfWeek = :day ORDER BY ws.startTime")
    List<WorkShift> findByCollectorIdAndDay(@Param("collectorId") Long collectorId,
                                            @Param("day") DayOfWeekEnum day);
}
