package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.BankFileImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankFileImportRepository extends JpaRepository<BankFileImport, Long> {
    Optional<BankFileImport> findByFileHash(String fileHash);
    boolean existsByFileHash(String fileHash);
    List<BankFileImport> findAllByOrderByCreatedAtDesc();
}
