package com.eaglepoint.workforce.repository;

import com.eaglepoint.workforce.entity.BankEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankEntryRepository extends JpaRepository<BankEntry, Long> {
    List<BankEntry> findByBankFileImportId(Long importId);
    List<BankEntry> findByMatchedFalse();
    List<BankEntry> findByBankReference(String bankReference);
}
