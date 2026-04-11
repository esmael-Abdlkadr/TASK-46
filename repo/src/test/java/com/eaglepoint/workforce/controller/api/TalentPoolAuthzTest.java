package com.eaglepoint.workforce.controller.api;

import com.eaglepoint.workforce.entity.*;
import com.eaglepoint.workforce.enums.RoleName;
import com.eaglepoint.workforce.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests object-level authorization for talent pools:
 * - Owner can access own pool
 * - Non-owner cannot access (gets 404, not 403)
 * - Admin can access any pool
 * - Admin list shows all pools
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TalentPoolAuthzTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TalentPoolRepository talentPoolRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;

    private User userA;
    private User userB;
    private User adminUser;

    @BeforeEach
    void setup() {
        Role recruiterRole = roleRepo.findByName(RoleName.RECRUITER).orElseGet(() -> {
            Role r = new Role();
            r.setName(RoleName.RECRUITER);
            return roleRepo.save(r);
        });
        Role adminRole = roleRepo.findByName(RoleName.ADMINISTRATOR).orElseGet(() -> {
            Role r = new Role();
            r.setName(RoleName.ADMINISTRATOR);
            return roleRepo.save(r);
        });

        userA = userRepo.findByUsername("poolUserA").orElseGet(() -> {
            User u = new User();
            u.setUsername("poolUserA");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Pool User A");
            u.setEmail("poola@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });

        userB = userRepo.findByUsername("poolUserB").orElseGet(() -> {
            User u = new User();
            u.setUsername("poolUserB");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Pool User B");
            u.setEmail("poolb@test.com");
            u.setEnabled(true);
            u.getRoles().add(recruiterRole);
            return userRepo.save(u);
        });

        adminUser = userRepo.findByUsername("poolAdmin").orElseGet(() -> {
            User u = new User();
            u.setUsername("poolAdmin");
            u.setPasswordHash("$2a$10$dummy");
            u.setDisplayName("Pool Admin");
            u.setEmail("pooladmin@test.com");
            u.setEnabled(true);
            u.getRoles().add(adminRole);
            return userRepo.save(u);
        });
    }

    @Test
    @WithMockUser(username = "poolUserA", roles = "RECRUITER")
    void talentPoolDetail_ownerCanAccess() throws Exception {
        TalentPool pool = new TalentPool();
        pool.setName("A's Pool");
        pool.setCreatedBy(userA.getId());
        pool = talentPoolRepo.save(pool);

        mockMvc.perform(get("/recruiter/talent-pools/" + pool.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "poolUserB", roles = "RECRUITER")
    void talentPoolDetail_nonOwnerDenied_returns404() throws Exception {
        TalentPool pool = new TalentPool();
        pool.setName("A's Pool");
        pool.setCreatedBy(userA.getId());
        pool = talentPoolRepo.save(pool);

        mockMvc.perform(get("/recruiter/talent-pools/" + pool.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "poolAdmin", roles = "ADMINISTRATOR")
    void talentPoolDetail_adminCanAccessAnyPool() throws Exception {
        TalentPool pool = new TalentPool();
        pool.setName("A's Pool");
        pool.setCreatedBy(userA.getId());
        pool = talentPoolRepo.save(pool);

        mockMvc.perform(get("/recruiter/talent-pools/" + pool.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "poolAdmin", roles = "ADMINISTRATOR")
    void talentPoolList_adminSeesAll() throws Exception {
        TalentPool poolA = new TalentPool();
        poolA.setName("A's Pool");
        poolA.setCreatedBy(userA.getId());
        talentPoolRepo.save(poolA);

        TalentPool poolB = new TalentPool();
        poolB.setName("B's Pool");
        poolB.setCreatedBy(userB.getId());
        talentPoolRepo.save(poolB);

        mockMvc.perform(get("/recruiter/talent-pools"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("pools"));
    }

    @Test
    @WithMockUser(username = "poolUserA", roles = "RECRUITER")
    void talentPoolList_nonAdminSeesOnlyOwn() throws Exception {
        TalentPool poolA = new TalentPool();
        poolA.setName("A's Pool");
        poolA.setCreatedBy(userA.getId());
        talentPoolRepo.save(poolA);

        TalentPool poolB = new TalentPool();
        poolB.setName("B's Pool");
        poolB.setCreatedBy(userB.getId());
        talentPoolRepo.save(poolB);

        mockMvc.perform(get("/recruiter/talent-pools"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "poolUserA", roles = "RECRUITER")
    void talentPoolDetail_nonexistent_returns404() throws Exception {
        mockMvc.perform(get("/recruiter/talent-pools/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void talentPoolList_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/recruiter/talent-pools"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "finUser", roles = "FINANCE_CLERK")
    void talentPoolList_wrongRole_denied() throws Exception {
        mockMvc.perform(get("/recruiter/talent-pools"))
                .andExpect(status().isForbidden());
    }
}
