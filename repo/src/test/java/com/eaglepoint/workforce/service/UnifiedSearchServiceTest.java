package com.eaglepoint.workforce.service;

import com.eaglepoint.workforce.dto.UnifiedSearchRequest;
import com.eaglepoint.workforce.dto.UnifiedSearchResult;
import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.entity.Department;
import com.eaglepoint.workforce.enums.SearchDomain;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
import com.eaglepoint.workforce.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnifiedSearchServiceTest {

    @Autowired
    private UnifiedSearchService searchService;

    @Autowired
    private CandidateProfileRepository candidateRepo;

    @Autowired
    private DepartmentRepository departmentRepo;

    @BeforeEach
    void setUp() {
        CandidateProfile c = new CandidateProfile();
        c.setFirstName("SearchTest");
        c.setLastName("User");
        c.setLocation("Chicago");
        c.setCurrentTitle("Engineer");
        candidateRepo.save(c);

        Department d = new Department();
        d.setCode("ENG");
        d.setName("Engineering");
        d.setHeadName("Alice");
        departmentRepo.save(d);
    }

    @Test
    void searchAcrossAllDomains() {
        UnifiedSearchRequest req = new UnifiedSearchRequest();
        req.setKeyword("SearchTest");

        List<UnifiedSearchResult> results = searchService.search(req);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.getDomain() == SearchDomain.CANDIDATES));
    }

    @Test
    void searchBySingleDomain() {
        UnifiedSearchRequest req = new UnifiedSearchRequest();
        req.setKeyword("Engineering");
        req.setDomains(Set.of(SearchDomain.DEPARTMENTS));

        List<UnifiedSearchResult> results = searchService.search(req);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(r -> r.getDomain() == SearchDomain.DEPARTMENTS));
    }

    @Test
    void searchWithLocationFilter() {
        UnifiedSearchRequest req = new UnifiedSearchRequest();
        req.setDomains(Set.of(SearchDomain.CANDIDATES));
        req.setLocationFilter("Chicago");

        List<UnifiedSearchResult> results = searchService.search(req);
        assertFalse(results.isEmpty());

        req.setLocationFilter("Tokyo");
        results = searchService.search(req);
        assertTrue(results.isEmpty());
    }

    @Test
    void emptyKeywordReturnsAll() {
        UnifiedSearchRequest req = new UnifiedSearchRequest();
        req.setDomains(Set.of(SearchDomain.DEPARTMENTS));

        List<UnifiedSearchResult> results = searchService.search(req);
        assertFalse(results.isEmpty());
    }
}
