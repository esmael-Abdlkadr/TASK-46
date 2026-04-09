package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.MetricVersion;
import com.eaglepoint.workforce.enums.AuditAction;
import com.eaglepoint.workforce.service.MetricDefinitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class MetricsApiController {

    private final MetricDefinitionService metricService;

    public MetricsApiController(MetricDefinitionService metricService) {
        this.metricService = metricService;
    }

    @PostMapping("/versions/{versionId}/publish")
    @Audited(action = AuditAction.UPDATE, resource = "API:MetricPublish")
    public ResponseEntity<MetricVersion> publish(@PathVariable Long versionId, Authentication auth) {
        return ResponseEntity.ok(metricService.publishVersion(versionId, auth.getName()));
    }

    @PostMapping("/{id}/rollback")
    @Audited(action = AuditAction.UPDATE, resource = "API:MetricRollback")
    public ResponseEntity<MetricVersion> rollback(@PathVariable Long id,
                                                    @RequestBody Map<String, Integer> body,
                                                    Authentication auth) {
        Integer targetVersion = body.get("targetVersion");
        return ResponseEntity.ok(metricService.rollbackToVersion(id, targetVersion, auth.getName()));
    }

    @GetMapping("/{id}/versions")
    @Audited(action = AuditAction.READ, resource = "API:MetricVersions")
    public ResponseEntity<?> versions(@PathVariable Long id) {
        return ResponseEntity.ok(metricService.getVersionHistory(id));
    }
}
