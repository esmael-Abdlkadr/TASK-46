package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.ImportJob;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.ImportService;
import com.eaglepoint.workforce.service.ResourceAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportApiController {

    private final ImportService importService;
    private final ResourceAuthorizationService authzService;

    public ImportApiController(ImportService importService, ResourceAuthorizationService authzService) {
        this.importService = importService;
        this.authzService = authzService;
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "API:ImportDetail")
    public ResponseEntity<ImportJob> detail(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(authzService.authorizeImportJob(id, auth));
    }
}
