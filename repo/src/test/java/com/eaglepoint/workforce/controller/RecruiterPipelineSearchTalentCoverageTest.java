package com.eaglepoint.workforce.controller;

import com.eaglepoint.workforce.dto.BatchMoveResult;
import com.eaglepoint.workforce.dto.MatchSearchRequest;
import com.eaglepoint.workforce.entity.CandidateProfile;
import com.eaglepoint.workforce.entity.TalentPool;
import com.eaglepoint.workforce.enums.PipelineStage;
import com.eaglepoint.workforce.repository.CandidateProfileRepository;
import com.eaglepoint.workforce.repository.TalentPoolRepository;
import com.eaglepoint.workforce.repository.UserRepository;
import com.eaglepoint.workforce.service.PipelineService;
import com.eaglepoint.workforce.service.SavedSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecruiterPipelineSearchTalentCoverageTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SavedSearchService savedSearchService;
    @Autowired
    private CandidateProfileRepository candidateRepository;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private TalentPoolRepository talentPoolRepository;

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    void searchExecute_save_loadSaved() throws Exception {
        mockMvc.perform(post("/recruiter/search/execute").with(csrf())
                        .param("keyword", "java"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/recruiter/search/save").with(csrf())
                        .param("searchName", "saved-" + Long.toHexString(System.nanoTime()))
                        .param("keyword", "spring"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/search"));

        Long uid = userRepository.findByUsername("recruiter").orElseThrow().getId();
        var ss = savedSearchService.save("load-me-" + Long.toHexString(System.nanoTime()),
                new MatchSearchRequest(), uid);

        mockMvc.perform(get("/recruiter/search/saved/" + ss.getId() + "/load"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    void pipelineBatchMove_postsForm() throws Exception {
        CandidateProfile c = new CandidateProfile();
        c.setFirstName("Pipe");
        c.setLastName("Line");
        c.setPipelineStage(PipelineStage.SOURCED);
        c = candidateRepository.save(c);

        mockMvc.perform(post("/recruiter/pipeline/batch-move").with(csrf())
                        .param("candidateIds", String.valueOf(c.getId()))
                        .param("targetStage", PipelineStage.SCREENING.name()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/candidates"));
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    void pipelineUndo_postsAfterServiceBatch() throws Exception {
        CandidateProfile c = new CandidateProfile();
        c.setFirstName("Undo");
        c.setLastName("Me");
        c.setPipelineStage(PipelineStage.SOURCED);
        c = candidateRepository.save(c);

        Long uid = userRepository.findByUsername("recruiter").orElseThrow().getId();
        BatchMoveResult result = pipelineService.batchMoveStage(
                List.of(c.getId()), PipelineStage.SCREENING, uid);

        mockMvc.perform(post("/recruiter/pipeline/undo/" + result.getBatchId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/candidates"));
    }

    @Test
    @WithMockUser(username = "recruiter", roles = "RECRUITER")
    void talentPool_create_add_remove_delete() throws Exception {
        String poolName = "PoolCov-" + Long.toHexString(System.nanoTime());
        mockMvc.perform(post("/recruiter/talent-pools/new").with(csrf())
                        .param("name", poolName)
                        .param("description", "coverage"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/talent-pools"));

        Long uid = userRepository.findByUsername("recruiter").orElseThrow().getId();
        TalentPool pool = talentPoolRepository.findByCreatedBy(uid).stream()
                .filter(p -> poolName.equals(p.getName()))
                .findFirst()
                .orElseThrow();

        CandidateProfile c = new CandidateProfile();
        c.setFirstName("Pool");
        c.setLastName("Cand");
        c.setPipelineStage(PipelineStage.SOURCED);
        c = candidateRepository.save(c);

        mockMvc.perform(post("/recruiter/talent-pools/" + pool.getId() + "/add-candidates").with(csrf())
                        .param("candidateIds", String.valueOf(c.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/talent-pools/" + pool.getId()));

        mockMvc.perform(post("/recruiter/talent-pools/" + pool.getId() + "/remove-candidates").with(csrf())
                        .param("candidateIds", String.valueOf(c.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/talent-pools/" + pool.getId()));

        mockMvc.perform(post("/recruiter/talent-pools/" + pool.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recruiter/talent-pools"));
    }
}
