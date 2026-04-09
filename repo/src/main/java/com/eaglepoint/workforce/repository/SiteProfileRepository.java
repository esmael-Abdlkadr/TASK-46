package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.SiteProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiteProfileRepository extends JpaRepository<SiteProfile, Long> {

    List<SiteProfile> findByActiveTrue();

    List<SiteProfile> findByZone(String zone);

    List<SiteProfile> findByActiveTrueOrderByNameAsc();
}
