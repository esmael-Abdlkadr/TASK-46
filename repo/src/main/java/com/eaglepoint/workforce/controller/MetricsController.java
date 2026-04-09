package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.audit.Audited;
import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.*;
import com.eaglepoint.workforce.service.MetricDefinitionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/metrics")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class MetricsController {

    private final MetricDefinitionService metricService;

    public MetricsController(MetricDefinitionService metricService) {
        this.metricService = metricService;
    }

    // === Metric Definitions ===

    @GetMapping
    @Audited(action = AuditAction.READ, resource = "MetricList")
    public String list(Model model) {
        model.addAttribute("metrics", metricService.findAll());
        model.addAttribute("baseMetrics", metricService.findBaseMetrics());
        model.addAttribute("derivedMetrics", metricService.findDerivedMetrics());
        return "admin/metrics/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dataTypes", MetricDataType.values());
        model.addAttribute("aggregationTypes", AggregationType.values());
        return "admin/metrics/metric-form";
    }

    @PostMapping("/new")
    @Audited(action = AuditAction.CREATE, resource = "MetricDefinition")
    public String create(@RequestParam String slug, @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam MetricDataType dataType,
                         @RequestParam AggregationType aggregationType,
                         @RequestParam(required = false) String sourceTable,
                         @RequestParam(required = false) String sourceColumn,
                         @RequestParam(required = false) String filterExpression,
                         @RequestParam(required = false) String unit,
                         @RequestParam(required = false) boolean derived,
                         @RequestParam(required = false) String formula,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttrs) {
        MetricDefinition metric = new MetricDefinition();
        metric.setSlug(slug);
        metric.setName(name);
        metric.setDescription(description);
        metric.setDataType(dataType);
        metric.setAggregationType(aggregationType);
        metric.setSourceTable(sourceTable);
        metric.setSourceColumn(sourceColumn);
        metric.setFilterExpression(filterExpression);
        metric.setUnit(unit);
        metric.setDerived(derived);
        metric.setFormula(formula);
        metric.setOwnerUsername(userDetails.getUsername());
        metricService.save(metric);
        redirectAttrs.addFlashAttribute("success", "Metric '" + name + "' created");
        return "redirect:/admin/metrics";
    }

    @GetMapping("/{id}")
    @Audited(action = AuditAction.READ, resource = "MetricDetail")
    public String detail(@PathVariable Long id, Model model) {
        MetricDefinition metric = metricService.findById(id)
                .orElseThrow(() -> new RuntimeException("Metric not found"));
        model.addAttribute("metric", metric);
        model.addAttribute("versions", metricService.getVersionHistory(id));
        model.addAttribute("currentVersion", metricService.getCurrentPublishedVersion(id).orElse(null));
        model.addAttribute("sources", metricService.getSourceMetrics(id));
        model.addAttribute("dependents", metricService.getDependentMetrics(id));
        model.addAttribute("downstreamImpact", metricService.getFullDownstreamImpact(id));
        model.addAttribute("windowCalcs", metricService.getWindowCalculations(id));
        model.addAttribute("allMetrics", metricService.findAll());
        return "admin/metrics/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        MetricDefinition metric = metricService.findById(id)
                .orElseThrow(() -> new RuntimeException("Metric not found"));
        model.addAttribute("metric", metric);
        model.addAttribute("dataTypes", MetricDataType.values());
        model.addAttribute("aggregationTypes", AggregationType.values());
        return "admin/metrics/metric-form";
    }

    @PostMapping("/{id}/edit")
    @Audited(action = AuditAction.UPDATE, resource = "MetricDefinition")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam MetricDataType dataType,
                         @RequestParam AggregationType aggregationType,
                         @RequestParam(required = false) String sourceTable,
                         @RequestParam(required = false) String sourceColumn,
                         @RequestParam(required = false) String filterExpression,
                         @RequestParam(required = false) String unit,
                         @RequestParam(required = false) boolean derived,
                         @RequestParam(required = false) String formula,
                         RedirectAttributes redirectAttrs) {
        MetricDefinition metric = metricService.findById(id)
                .orElseThrow(() -> new RuntimeException("Metric not found"));
        metric.setName(name);
        metric.setDescription(description);
        metric.setDataType(dataType);
        metric.setAggregationType(aggregationType);
        metric.setSourceTable(sourceTable);
        metric.setSourceColumn(sourceColumn);
        metric.setFilterExpression(filterExpression);
        metric.setUnit(unit);
        metric.setDerived(derived);
        metric.setFormula(formula);
        metricService.save(metric);
        redirectAttrs.addFlashAttribute("success", "Metric updated");
        return "redirect:/admin/metrics/" + id;
    }

    // === Versioning ===

    @PostMapping("/{id}/versions/draft")
    @Audited(action = AuditAction.CREATE, resource = "MetricVersionDraft")
    public String createDraft(@PathVariable Long id,
                               @RequestParam(required = false) String changeDescription,
                               RedirectAttributes redirectAttrs) {
        metricService.createDraftVersion(id, changeDescription);
        redirectAttrs.addFlashAttribute("success", "Draft version created");
        return "redirect:/admin/metrics/" + id;
    }

    @PostMapping("/versions/{versionId}/publish")
    @Audited(action = AuditAction.UPDATE, resource = "MetricVersionPublish")
    public String publishVersion(@PathVariable Long versionId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttrs) {
        MetricVersion version = metricService.publishVersion(versionId, userDetails.getUsername());
        redirectAttrs.addFlashAttribute("success",
                "Version " + version.getVersionNumber() + " published by " + userDetails.getUsername());
        return "redirect:/admin/metrics/" + version.getMetricDefinition().getId();
    }

    @PostMapping("/{id}/versions/rollback")
    @Audited(action = AuditAction.UPDATE, resource = "MetricVersionRollback")
    public String rollback(@PathVariable Long id,
                            @RequestParam Integer targetVersion,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttrs) {
        metricService.rollbackToVersion(id, targetVersion, userDetails.getUsername());
        redirectAttrs.addFlashAttribute("success", "Rolled back to version " + targetVersion);
        return "redirect:/admin/metrics/" + id;
    }

    @GetMapping("/versions/{versionId}")
    @Audited(action = AuditAction.READ, resource = "MetricVersionDetail")
    public String versionDetail(@PathVariable Long versionId, Model model) {
        MetricVersion version = metricService.findVersionById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found"));
        model.addAttribute("version", version);
        return "admin/metrics/version-detail";
    }

    // === Lineage ===

    @PostMapping("/{id}/lineage/add")
    @Audited(action = AuditAction.CREATE, resource = "MetricLineage")
    public String addLineage(@PathVariable Long id,
                              @RequestParam Long sourceMetricId,
                              @RequestParam String relationshipType,
                              @RequestParam(required = false) String contributionDescription,
                              RedirectAttributes redirectAttrs) {
        try {
            metricService.addLineage(id, sourceMetricId, relationshipType, contributionDescription);
            redirectAttrs.addFlashAttribute("success", "Lineage relationship added");
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/metrics/" + id;
    }

    @PostMapping("/lineage/{lineageId}/remove")
    @Audited(action = AuditAction.DELETE, resource = "MetricLineage")
    public String removeLineage(@PathVariable Long lineageId,
                                 @RequestParam Long metricId,
                                 RedirectAttributes redirectAttrs) {
        metricService.removeLineage(lineageId);
        redirectAttrs.addFlashAttribute("success", "Lineage relationship removed");
        return "redirect:/admin/metrics/" + metricId;
    }

    // === Dimensions ===

    @GetMapping("/dimensions")
    @Audited(action = AuditAction.READ, resource = "DimensionList")
    public String dimensions(Model model) {
        model.addAttribute("dimensions", metricService.findAllDimensions());
        return "admin/metrics/dimensions";
    }

    @PostMapping("/dimensions/save")
    @Audited(action = AuditAction.CREATE, resource = "DimensionDefinition")
    public String saveDimension(@RequestParam String slug, @RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) String sourceTable,
                                 @RequestParam(required = false) String sourceColumn,
                                 @RequestParam(required = false) String hierarchyLevel,
                                 RedirectAttributes redirectAttrs) {
        DimensionDefinition dim = new DimensionDefinition();
        dim.setSlug(slug);
        dim.setName(name);
        dim.setDescription(description);
        dim.setSourceTable(sourceTable);
        dim.setSourceColumn(sourceColumn);
        dim.setHierarchyLevel(hierarchyLevel);
        metricService.saveDimension(dim);
        redirectAttrs.addFlashAttribute("success", "Dimension '" + name + "' saved");
        return "redirect:/admin/metrics/dimensions";
    }

    @PostMapping("/{id}/dimensions/link")
    @Audited(action = AuditAction.UPDATE, resource = "MetricDimensionLink")
    public String linkDimension(@PathVariable Long id,
                                 @RequestParam Long dimensionId,
                                 @RequestParam(required = false) String joinExpression,
                                 RedirectAttributes redirectAttrs) {
        metricService.linkDimension(id, dimensionId, joinExpression);
        redirectAttrs.addFlashAttribute("success", "Dimension linked to metric");
        return "redirect:/admin/metrics/" + id;
    }
}
