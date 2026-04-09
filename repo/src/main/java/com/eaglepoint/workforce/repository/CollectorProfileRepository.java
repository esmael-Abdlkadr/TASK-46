package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.CollectorProfile;
import com.eaglepoint.workforce.enums.CollectorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollectorProfileRepository extends JpaRepository<CollectorProfile, Long> {

    List<CollectorProfile> findByStatus(CollectorStatus status);

    List<CollectorProfile> findByZone(String zone);

    Optional<CollectorProfile> findByEmployeeId(String employeeId);

    @Query("SELECT c FROM CollectorProfile c LEFT JOIN FETCH c.workShifts WHERE c.id = :id")
    Optional<CollectorProfile> findByIdWithShifts(@Param("id") Long id);

    @Query("SELECT c FROM CollectorProfile c WHERE c.status = 'AVAILABLE' AND c.zone = :zone")
    List<CollectorProfile> findAvailableByZone(@Param("zone") String zone);

    @Query("SELECT c FROM CollectorProfile c WHERE c.status = 'AVAILABLE'")
    List<CollectorProfile> findAllAvailable();

    long countByStatus(CollectorStatus status);
}
