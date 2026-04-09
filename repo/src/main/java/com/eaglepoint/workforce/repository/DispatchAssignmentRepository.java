package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.DispatchAssignment;
import com.eaglepoint.workforce.enums.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DispatchAssignmentRepository extends JpaRepository<DispatchAssignment, Long> {

    List<DispatchAssignment> findByStatus(DispatchStatus status);

    List<DispatchAssignment> findByCollectorIdAndStatusIn(Long collectorId, List<DispatchStatus> statuses);

    List<DispatchAssignment> findBySiteId(Long siteId);

    @Query("SELECT d FROM DispatchAssignment d WHERE d.status = 'OFFERED' " +
           "AND d.acceptanceExpiresAt < :now")
    List<DispatchAssignment> findExpiredOffers(@Param("now") LocalDateTime now);

    @Query("SELECT d FROM DispatchAssignment d WHERE d.status IN ('PENDING', 'OFFERED') " +
           "AND d.site.id = :siteId")
    List<DispatchAssignment> findPendingBySiteId(@Param("siteId") Long siteId);

    @Query("SELECT d FROM DispatchAssignment d LEFT JOIN FETCH d.site LEFT JOIN FETCH d.collector " +
           "WHERE d.status IN :statuses ORDER BY d.createdAt DESC")
    List<DispatchAssignment> findByStatusInWithDetails(@Param("statuses") List<DispatchStatus> statuses);

    long countByStatus(DispatchStatus status);

    @Query("SELECT COUNT(d) FROM DispatchAssignment d WHERE d.collector.id = :collectorId " +
           "AND d.status IN ('ACCEPTED', 'IN_PROGRESS')")
    long countActiveByCollectorId(@Param("collectorId") Long collectorId);

    @Query("SELECT d FROM DispatchAssignment d LEFT JOIN FETCH d.site LEFT JOIN FETCH d.collector " +
           "ORDER BY d.createdAt DESC")
    List<DispatchAssignment> findAllWithDetails();
}
