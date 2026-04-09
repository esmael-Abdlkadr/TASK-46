package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.SearchSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SearchSnapshotRepository extends JpaRepository<SearchSnapshot, Long> {

    List<SearchSnapshot> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    @Query("SELECT ss FROM SearchSnapshot ss LEFT JOIN FETCH ss.results WHERE ss.id = :id")
    Optional<SearchSnapshot> findByIdWithResults(@Param("id") Long id);

    @Query("SELECT ss FROM SearchSnapshot ss LEFT JOIN FETCH ss.results WHERE ss.id = :id AND ss.createdBy = :userId")
    Optional<SearchSnapshot> findByIdAndCreatedByWithResults(@Param("id") Long id, @Param("userId") Long userId);

    Optional<SearchSnapshot> findByIdAndCreatedBy(Long id, Long createdBy);
}
