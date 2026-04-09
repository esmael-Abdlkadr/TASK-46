package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.exception.AccessDeniedException;
import com.eaglepoint.workforce.exception.ResourceNotFoundException;
import com.eaglepoint.workforce.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Centralized object-level authorization.
 * Policy: admin can access all; non-admin only own records. Return 404 for unauthorized.
 */
@Service
public class ResourceAuthorizationService {

    private final UserService userService;
    private final SavedSearchRepository savedSearchRepo;
    private final SearchSnapshotRepository snapshotRepo;
    private final TalentPoolRepository talentPoolRepo;
    private final ExportJobRepository exportJobRepo;
    private final ImportJobRepository importJobRepo;
    private final PaymentTransactionRepository paymentRepo;

    public ResourceAuthorizationService(UserService userService,
                                         SavedSearchRepository savedSearchRepo,
                                         SearchSnapshotRepository snapshotRepo,
                                         TalentPoolRepository talentPoolRepo,
                                         ExportJobRepository exportJobRepo,
                                         ImportJobRepository importJobRepo,
                                         PaymentTransactionRepository paymentRepo) {
        this.userService = userService;
        this.savedSearchRepo = savedSearchRepo;
        this.snapshotRepo = snapshotRepo;
        this.talentPoolRepo = talentPoolRepo;
        this.exportJobRepo = exportJobRepo;
        this.importJobRepo = importJobRepo;
        this.paymentRepo = paymentRepo;
    }

    public boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMINISTRATOR"));
    }

    public Long resolveUserId(Authentication auth) {
        return userService.findByUsername(auth.getName())
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public SavedSearch authorizeSavedSearch(Long id, Authentication auth) {
        if (isAdmin(auth)) {
            return savedSearchRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Saved search not found"));
        }
        Long userId = resolveUserId(auth);
        return savedSearchRepo.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved search not found"));
    }

    @Transactional(readOnly = true)
    public SearchSnapshot authorizeSnapshot(Long id, Authentication auth) {
        if (isAdmin(auth)) {
            return snapshotRepo.findByIdWithResults(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Search snapshot not found"));
        }
        Long userId = resolveUserId(auth);
        return snapshotRepo.findByIdAndCreatedByWithResults(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Search snapshot not found"));
    }

    @Transactional(readOnly = true)
    public TalentPool authorizeTalentPool(Long id, Authentication auth) {
        if (isAdmin(auth)) {
            return talentPoolRepo.findByIdWithCandidates(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Talent pool not found"));
        }
        Long userId = resolveUserId(auth);
        return talentPoolRepo.findByIdAndCreatedByWithCandidates(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Talent pool not found"));
    }

    @Transactional(readOnly = true)
    public ExportJob authorizeExportJob(Long id, Authentication auth) {
        if (isAdmin(auth)) {
            return exportJobRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Export job not found"));
        }
        Long userId = resolveUserId(auth);
        return exportJobRepo.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Export job not found"));
    }

    @Transactional(readOnly = true)
    public ImportJob authorizeImportJob(Long id, Authentication auth) {
        if (isAdmin(auth)) {
            return importJobRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));
        }
        Long userId = resolveUserId(auth);
        return importJobRepo.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));
    }

    @Transactional(readOnly = true)
    public PaymentTransaction authorizePayment(Long id, Authentication auth) {
        return paymentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }
}
