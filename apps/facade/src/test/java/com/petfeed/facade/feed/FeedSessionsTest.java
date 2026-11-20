package com.petfeed.facade.feed;

import com.petfeed.contracts.dto.response.FeedCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedSessionsTest {
    @Test void bindsCursorToOwnerAndSnapshotAndExpiresWithoutRestartingSilently() {
        var clock = mock(Clock.class);
        var now = Instant.parse("2026-11-20T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        var sessions = new FeedSessions(clock);
        var ordered = IntStream.range(0, 25).mapToObj(i -> new FeedCandidate((long) i, "a", now)).toList();
        var page = sessions.create("owner", ordered, Set.of());
        assertThat(sessions.resume("owner", page.id() + ":10").snapshot()).isEqualTo(page.snapshot());
        assertThatThrownBy(() -> sessions.resume("other", page.id() + ":10"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        for (var cursor : List.of("bad", page.id() + ":1", page.id() + ":30", page.id() + ":-1"))
            assertThatThrownBy(() -> sessions.resume("owner", cursor)).isInstanceOf(ResponseStatusException.class);
        when(clock.instant()).thenReturn(now.plusSeconds(900));
        assertThatThrownBy(() -> sessions.resume("owner", page.id() + ":10"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(410));
    }
}
