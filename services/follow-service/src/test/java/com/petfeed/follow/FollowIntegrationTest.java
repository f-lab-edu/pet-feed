package com.petfeed.follow;

import com.petfeed.follow.domain.FollowRelation;
import com.petfeed.follow.repository.FollowRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FollowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired FollowRepository follows;
    @BeforeEach void reset() { follows.deleteAll(); }

    @Test void followUnfollowAndDuplicateRules() throws Exception {
        mvc.perform(post("/api/v1/following/target").header("X-User-Key", "owner")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/following/target").header("X-User-Key", "owner")).andExpect(status().isConflict());
        assertThat(follows.existsByFollowerKeyAndFollowingKey("owner", "target")).isTrue();
        assertThat(follows.existsByFollowerKeyAndFollowingKey("target", "owner")).isFalse();
        for (int i = 0; i < 2; i++) mvc.perform(delete("/api/v1/following/target").header("X-User-Key", "owner"))
                .andExpect(status().isOk());
        assertThat(follows.count()).isZero();
    }

    @Test void selfAndUnauthenticatedWritesAreRejected() throws Exception {
        mvc.perform(post("/api/v1/following/owner").header("X-User-Key", "owner")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/following/target")).andExpect(status().isBadRequest());
        mvc.perform(get("/internal/v1/users/owner/followers").param("cursor", "-1")).andExpect(status().isBadRequest());
        assertThat(follows.count()).isZero();
    }

    @Test void bothDirectionsUseRelationCursorAndBatchFlags() throws Exception {
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 21; i++) ids.add(follows.save(FollowRelation.create("owner", "target" + i)).getId());
        follows.save(FollowRelation.create("other", "target20"));
        mvc.perform(get("/internal/v1/users/owner/followings")).andExpect(jsonPath("$.entries.length()").value(20))
                .andExpect(jsonPath("$.entries[0].userKey").value("target20"))
                .andExpect(jsonPath("$.nextCursor").value(ids.get(1))).andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/internal/v1/users/owner/followings").param("cursor", ids.get(1).toString()))
                .andExpect(jsonPath("$.entries.length()").value(1)).andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/internal/v1/users/target20/followers")).andExpect(jsonPath("$.entries.length()").value(2));
        mvc.perform(get("/internal/v1/follows/targets").param("requester", "owner").param("keys", "target20,missing"))
                .andExpect(jsonPath("$[0]").value("target20")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void uniqueDatabaseConstraintProtectsConcurrentRequests() throws Exception {
        var executor = Executors.newFixedThreadPool(6);
        var start = new CountDownLatch(1);
        try {
            var tasks = new java.util.ArrayList<Future<Integer>>();
            for (int i = 0; i < 6; i++) tasks.add(executor.submit(() -> {
                start.await();
                return mvc.perform(post("/api/v1/following/target").header("X-User-Key", "owner"))
                        .andReturn().getResponse().getStatus();
            }));
            start.countDown();
            var results = new java.util.ArrayList<Integer>();
            for (var task : tasks) results.add(task.get(20, TimeUnit.SECONDS));
            assertThat(results).containsOnly(200, 409);
            assertThat(results.stream().filter(code -> code == 200).count()).isEqualTo(1);
            assertThat(follows.count()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }
}
