package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.SavedSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedSearchRepository extends JpaRepository<SavedSearch, Long> {
    List<SavedSearch> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    Optional<SavedSearch> findByIdAndCreatedBy(Long id, Long createdBy);
}
