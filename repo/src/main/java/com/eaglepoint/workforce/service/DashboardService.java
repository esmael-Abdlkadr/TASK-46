package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.DashboardData;
import com.eaglepoint.workforce.enums.JobStatus;
import com.eaglepoint.workforce.enums.RequisitionStatus;
import com.eaglepoint.workforce.repository.ExceptionAlertRepository;
import com.eaglepoint.workforce.repository.JobRepository;
import com.eaglepoint.workforce.repository.RequisitionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final RequisitionRepository requisitionRepository;
    private final JobRepository jobRepository;
    private final ExceptionAlertRepository exceptionAlertRepository;

    public DashboardService(RequisitionRepository requisitionRepository,
                            JobRepository jobRepository,
                            ExceptionAlertRepository exceptionAlertRepository) {
        this.requisitionRepository = requisitionRepository;
        this.jobRepository = jobRepository;
        this.exceptionAlertRepository = exceptionAlertRepository;
    }

    @Transactional(readOnly = true)
    public DashboardData getDashboardData() {
        DashboardData data = new DashboardData();
        data.setOpenRequisitions(requisitionRepository.countByStatus(RequisitionStatus.OPEN));
        data.setUnassignedJobs(jobRepository.countByStatus(JobStatus.UNASSIGNED));
        data.setActiveAlerts(exceptionAlertRepository.countByResolvedFalse());
        data.setRecentRequisitions(
                requisitionRepository.findTop10ByStatusOrderByCreatedAtDesc(RequisitionStatus.OPEN));
        data.setRecentUnassignedJobs(
                jobRepository.findTop10ByStatusOrderByCreatedAtDesc(JobStatus.UNASSIGNED));
        data.setRecentAlerts(
                exceptionAlertRepository.findTop10ByResolvedFalseOrderByCreatedAtDesc());
        return data;
    }
}
