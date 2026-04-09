package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.exception.ResourceNotFoundException;
import com.eaglepoint.workforce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResourceAuthorizationTest {

    @Autowired private SavedSearchRepository savedSearchRepo;
    @Autowired private ExportJobRepository exportJobRepo;
    @Autowired private ImportJobRepository importJobRepo;

    @Test
    void savedSearch_ownerCanAccess() {
        SavedSearch ss = new SavedSearch();
        ss.setName("My Search");
        ss.setCreatedBy(100L);
        ss.setSearchCriteriaJson("{}");
        ss = savedSearchRepo.save(ss);

        assertTrue(savedSearchRepo.findByIdAndCreatedBy(ss.getId(), 100L).isPresent());
    }

    @Test
    void savedSearch_otherUserCannotAccess() {
        SavedSearch ss = new SavedSearch();
        ss.setName("Private Search");
        ss.setCreatedBy(100L);
        ss.setSearchCriteriaJson("{}");
        ss = savedSearchRepo.save(ss);

        assertTrue(savedSearchRepo.findByIdAndCreatedBy(ss.getId(), 999L).isEmpty());
    }

    @Test
    void exportJob_ownerCanAccess() {
        ExportJob ej = new ExportJob();
        ej.setName("My Export");
        ej.setExportType("candidates");
        ej.setFileFormat("csv");
        ej.setCreatedBy(200L);
        ej = exportJobRepo.save(ej);

        assertTrue(exportJobRepo.findByIdAndCreatedBy(ej.getId(), 200L).isPresent());
    }

    @Test
    void exportJob_otherUserCannotAccess() {
        ExportJob ej = new ExportJob();
        ej.setName("Private Export");
        ej.setExportType("candidates");
        ej.setFileFormat("csv");
        ej.setCreatedBy(200L);
        ej = exportJobRepo.save(ej);

        assertTrue(exportJobRepo.findByIdAndCreatedBy(ej.getId(), 999L).isEmpty());
    }

    @Test
    void importJob_ownerCanAccess() {
        ImportJob ij = new ImportJob();
        ij.setOriginalFileName("test.csv");
        ij.setImportType("departments");
        ij.setFileFingerprint("abc123unique");
        ij.setCreatedBy(300L);
        ij = importJobRepo.save(ij);

        assertTrue(importJobRepo.findByIdAndCreatedBy(ij.getId(), 300L).isPresent());
    }

    @Test
    void importJob_otherUserCannotAccess() {
        ImportJob ij = new ImportJob();
        ij.setOriginalFileName("test.csv");
        ij.setImportType("departments");
        ij.setFileFingerprint("def456unique");
        ij.setCreatedBy(300L);
        ij = importJobRepo.save(ij);

        assertTrue(importJobRepo.findByIdAndCreatedBy(ij.getId(), 999L).isEmpty());
    }
}
