package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.UnifiedSearchRequest;
import com.eaglepoint.workforce.dto.UnifiedSearchResult;
import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.SearchDomain;
import com.eaglepoint.workforce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UnifiedSearchService {

    private final CandidateProfileRepository candidateRepo;
    private final CollectorProfileRepository collectorRepo;
    private final SiteProfileRepository siteRepo;
    private final RequisitionRepository requisitionRepo;
    private final DispatchAssignmentRepository dispatchRepo;
    private final DepartmentRepository departmentRepo;
    private final TrainingCourseRepository courseRepo;
    private final MemberRepository memberRepo;
    private final EnterpriseRepository enterpriseRepo;
    private final ResourceRepository resourceRepo;
    private final ServiceOrderRepository orderRepo;
    private final RedemptionRecordRepository redemptionRepo;

    public UnifiedSearchService(CandidateProfileRepository candidateRepo,
                                 CollectorProfileRepository collectorRepo,
                                 SiteProfileRepository siteRepo,
                                 RequisitionRepository requisitionRepo,
                                 DispatchAssignmentRepository dispatchRepo,
                                 DepartmentRepository departmentRepo,
                                 TrainingCourseRepository courseRepo,
                                 MemberRepository memberRepo,
                                 EnterpriseRepository enterpriseRepo,
                                 ResourceRepository resourceRepo,
                                 ServiceOrderRepository orderRepo,
                                 RedemptionRecordRepository redemptionRepo) {
        this.candidateRepo = candidateRepo;
        this.collectorRepo = collectorRepo;
        this.siteRepo = siteRepo;
        this.requisitionRepo = requisitionRepo;
        this.dispatchRepo = dispatchRepo;
        this.departmentRepo = departmentRepo;
        this.courseRepo = courseRepo;
        this.memberRepo = memberRepo;
        this.enterpriseRepo = enterpriseRepo;
        this.resourceRepo = resourceRepo;
        this.orderRepo = orderRepo;
        this.redemptionRepo = redemptionRepo;
    }

    @Transactional(readOnly = true)
    public List<UnifiedSearchResult> search(UnifiedSearchRequest request) {
        List<UnifiedSearchResult> results = new ArrayList<>();
        Set<SearchDomain> domains = request.getDomains();
        if (domains == null || domains.isEmpty()) {
            domains = EnumSet.allOf(SearchDomain.class);
        }
        String kw = request.getKeyword();
        boolean hasKeyword = kw != null && !kw.isBlank();
        String kwLower = hasKeyword ? kw.toLowerCase() : "";

        if (domains.contains(SearchDomain.CANDIDATES)) {
            List<CandidateProfile> candidates = hasKeyword
                    ? candidateRepo.searchByKeyword(kw) : candidateRepo.findAll();
            for (CandidateProfile c : candidates) {
                if (matchesLocation(c.getLocation(), request.getLocationFilter())) {
                    results.add(new UnifiedSearchResult(SearchDomain.CANDIDATES, c.getId(),
                            c.getFullName(), c.getCurrentTitle(),
                            c.getPipelineStage().getDisplayName(),
                            "/recruiter/candidates/" + c.getId()));
                }
            }
        }

        if (domains.contains(SearchDomain.COLLECTORS)) {
            for (CollectorProfile c : collectorRepo.findAll()) {
                if (hasKeyword && !matches(c.getFullName(), kwLower)
                        && !matches(c.getEmployeeId(), kwLower)
                        && !matches(c.getZone(), kwLower)) continue;
                if (!matchesLocation(c.getZone(), request.getLocationFilter())) continue;
                results.add(new UnifiedSearchResult(SearchDomain.COLLECTORS, c.getId(),
                        c.getFullName(), "Employee: " + (c.getEmployeeId() != null ? c.getEmployeeId() : "-"),
                        c.getStatus().getDisplayName(),
                        "/dispatch/collectors/" + c.getId()));
            }
        }

        if (domains.contains(SearchDomain.SITES)) {
            for (SiteProfile s : siteRepo.findAll()) {
                if (hasKeyword && !matches(s.getName(), kwLower)
                        && !matches(s.getAddress(), kwLower)
                        && !matches(s.getZone(), kwLower)) continue;
                if (!matchesLocation(s.getZone(), request.getLocationFilter())) continue;
                results.add(new UnifiedSearchResult(SearchDomain.SITES, s.getId(),
                        s.getName(), s.getAddress(),
                        s.isActive() ? "Active" : "Inactive",
                        "/dispatch/sites/" + s.getId()));
            }
        }

        if (domains.contains(SearchDomain.REQUISITIONS)) {
            for (Requisition r : requisitionRepo.findAll()) {
                if (hasKeyword && !matches(r.getTitle(), kwLower)
                        && !matches(r.getDescription(), kwLower)) continue;
                if (request.getStatusFilter() != null && !request.getStatusFilter().isBlank()
                        && !r.getStatus().name().equalsIgnoreCase(request.getStatusFilter())) continue;
                results.add(new UnifiedSearchResult(SearchDomain.REQUISITIONS, r.getId(),
                        r.getTitle(), r.getDescription(),
                        r.getStatus().name(), "#"));
            }
        }

        if (domains.contains(SearchDomain.DISPATCH_ASSIGNMENTS)) {
            for (DispatchAssignment d : dispatchRepo.findAllWithDetails()) {
                if (hasKeyword && !matches(d.getTitle(), kwLower)
                        && !matches(d.getDescription(), kwLower)) continue;
                if (request.getStatusFilter() != null && !request.getStatusFilter().isBlank()
                        && !d.getStatus().name().equalsIgnoreCase(request.getStatusFilter())) continue;
                results.add(new UnifiedSearchResult(SearchDomain.DISPATCH_ASSIGNMENTS, d.getId(),
                        d.getTitle(), "Site: " + d.getSite().getName(),
                        d.getStatus().getDisplayName(),
                        "/dispatch/assignments/" + d.getId()));
            }
        }

        if (domains.contains(SearchDomain.DEPARTMENTS)) {
            for (Department dept : departmentRepo.findAll()) {
                if (hasKeyword && !matches(dept.getName(), kwLower)
                        && !matches(dept.getCode(), kwLower)) continue;
                results.add(new UnifiedSearchResult(SearchDomain.DEPARTMENTS, dept.getId(),
                        dept.getCode() + " - " + dept.getName(), dept.getHeadName(),
                        dept.isActive() ? "Active" : "Inactive",
                        "/masterdata/departments"));
            }
        }

        if (domains.contains(SearchDomain.TRAINING_COURSES)) {
            for (TrainingCourse tc : courseRepo.findAll()) {
                if (hasKeyword && !matches(tc.getName(), kwLower)
                        && !matches(tc.getCode(), kwLower)
                        && !matches(tc.getDescription(), kwLower)) continue;
                results.add(new UnifiedSearchResult(SearchDomain.TRAINING_COURSES, tc.getId(),
                        tc.getCode() + " - " + tc.getName(), tc.getDescription(),
                        tc.isActive() ? "Active" : "Inactive",
                        "/masterdata/courses"));
            }
        }

        if (domains.contains(SearchDomain.MEMBERS)) {
            List<Member> members = hasKeyword ? memberRepo.findByFullNameContainingIgnoreCase(kw) : memberRepo.findAll();
            for (Member m : members) {
                results.add(new UnifiedSearchResult(SearchDomain.MEMBERS, m.getId(),
                        m.getMemberCode() + " - " + m.getFullName(), m.getMembershipTier(),
                        m.isActive() ? "Active" : "Inactive", "#"));
            }
        }

        if (domains.contains(SearchDomain.ENTERPRISES)) {
            List<Enterprise> ents = hasKeyword ? enterpriseRepo.findByNameContainingIgnoreCase(kw) : enterpriseRepo.findAll();
            for (Enterprise e : ents) {
                if (!matchesLocation(e.getLocation(), request.getLocationFilter())) continue;
                results.add(new UnifiedSearchResult(SearchDomain.ENTERPRISES, e.getId(),
                        e.getEnterpriseCode() + " - " + e.getName(), e.getIndustry(),
                        e.isActive() ? "Active" : "Inactive", "#"));
            }
        }

        if (domains.contains(SearchDomain.RESOURCES)) {
            List<Resource> resources = hasKeyword ? resourceRepo.findByNameContainingIgnoreCase(kw) : resourceRepo.findAll();
            for (Resource r : resources) {
                if (!matchesLocation(r.getLocation(), request.getLocationFilter())) continue;
                results.add(new UnifiedSearchResult(SearchDomain.RESOURCES, r.getId(),
                        r.getResourceCode() + " - " + r.getName(), r.getResourceType(),
                        r.isAvailable() ? "Available" : "Unavailable", "#"));
            }
        }

        if (domains.contains(SearchDomain.ORDERS)) {
            List<ServiceOrder> orders = hasKeyword
                    ? orderRepo.findByOrderCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(kw, kw)
                    : orderRepo.findAll();
            for (ServiceOrder o : orders) {
                if (request.getStatusFilter() != null && !request.getStatusFilter().isBlank()
                        && !o.getStatus().equalsIgnoreCase(request.getStatusFilter())) continue;
                results.add(new UnifiedSearchResult(SearchDomain.ORDERS, o.getId(),
                        o.getOrderCode(), o.getDescription(), o.getStatus(), "#"));
            }
        }

        if (domains.contains(SearchDomain.REDEMPTIONS)) {
            List<RedemptionRecord> recs = hasKeyword
                    ? redemptionRepo.findByRedemptionCodeContainingIgnoreCaseOrItemDescriptionContainingIgnoreCase(kw, kw)
                    : redemptionRepo.findAll();
            for (RedemptionRecord r : recs) {
                results.add(new UnifiedSearchResult(SearchDomain.REDEMPTIONS, r.getId(),
                        r.getRedemptionCode(), r.getItemDescription(), r.getStatus(), "#"));
            }
        }

        return results;
    }

    private boolean matches(String field, String kwLower) {
        return field != null && field.toLowerCase().contains(kwLower);
    }

    private boolean matchesLocation(String field, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return field != null && field.toLowerCase().contains(filter.toLowerCase());
    }
}
