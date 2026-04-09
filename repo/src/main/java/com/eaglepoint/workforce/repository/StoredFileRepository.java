package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByFingerprint(String fingerprint);
    boolean existsByFingerprint(String fingerprint);
    List<StoredFile> findByCategory(String category);
    List<StoredFile> findAllByOrderByCreatedAtDesc();
}
