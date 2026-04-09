package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.SiteProfile;
import com.eaglepoint.workforce.repository.SiteProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SiteService {

    private final SiteProfileRepository siteRepository;

    public SiteService(SiteProfileRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Transactional(readOnly = true)
    public List<SiteProfile> findAll() {
        return siteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SiteProfile> findAllActive() {
        return siteRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<SiteProfile> findById(Long id) {
        return siteRepository.findById(id);
    }

    @Transactional
    public SiteProfile save(SiteProfile site) {
        if (site.getCapacityLimit() != null && site.getCapacityLimit() < 1) {
            throw new IllegalArgumentException("Capacity limit must be at least 1");
        }
        return siteRepository.save(site);
    }

    @Transactional
    public void incrementOccupancy(Long siteId) {
        SiteProfile site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site not found: " + siteId));
        if (site.isAtCapacity()) {
            throw new RuntimeException("Site " + site.getName() + " is at capacity (" +
                    site.getCapacityLimit() + ")");
        }
        site.setCurrentOccupancy(site.getCurrentOccupancy() + 1);
        siteRepository.save(site);
    }

    @Transactional
    public void decrementOccupancy(Long siteId) {
        SiteProfile site = siteRepository.findById(siteId)
                .orElseThrow(() -> new RuntimeException("Site not found: " + siteId));
        if (site.getCurrentOccupancy() > 0) {
            site.setCurrentOccupancy(site.getCurrentOccupancy() - 1);
            siteRepository.save(site);
        }
    }
}
