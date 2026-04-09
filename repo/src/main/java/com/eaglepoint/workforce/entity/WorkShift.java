package com.eaglepoint.workforce.entity;

import com.eaglepoint.workforce.enums.DayOfWeekEnum;
import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "work_shifts", indexes = {
    @Index(name = "idx_shift_collector", columnList = "collector_id"),
    @Index(name = "idx_shift_day", columnList = "day_of_week")
})
public class WorkShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id", nullable = false)
    private CollectorProfile collector;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeekEnum dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private SiteProfile site;

    public WorkShift() {}

    public WorkShift(CollectorProfile collector, DayOfWeekEnum dayOfWeek,
                     LocalTime startTime, LocalTime endTime, SiteProfile site) {
        this.collector = collector;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.site = site;
    }

    public String getFormattedTimeRange() {
        return formatTime(startTime) + " - " + formatTime(endTime);
    }

    private String formatTime(LocalTime t) {
        int hour = t.getHour();
        int min = t.getMinute();
        String ampm = hour >= 12 ? "PM" : "AM";
        int h12 = hour % 12;
        if (h12 == 0) h12 = 12;
        return String.format("%d:%02d %s", h12, min, ampm);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CollectorProfile getCollector() { return collector; }
    public void setCollector(CollectorProfile collector) { this.collector = collector; }
    public DayOfWeekEnum getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeekEnum dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public SiteProfile getSite() { return site; }
    public void setSite(SiteProfile site) { this.site = site; }
}
