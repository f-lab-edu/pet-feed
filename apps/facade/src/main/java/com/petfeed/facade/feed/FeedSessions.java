package com.petfeed.facade.feed;

import com.petfeed.contracts.dto.response.FeedCandidate;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** Bounded, process-local snapshots; deployment behind multiple instances needs shared storage. */
@Component
public class FeedSessions {

    public record Snapshot(String owner, List<FeedCandidate> ordered, Set<String> following, Instant expires) {}
    public record Page(String id, Snapshot snapshot, int offset) {}
    private final Map<String, Snapshot> sessions = new LinkedHashMap<>();
    private final Clock clock;

    public FeedSessions() { this(Clock.systemUTC()); }
    FeedSessions(Clock clock) { this.clock = clock; }

    public synchronized Page create(String owner, List<FeedCandidate> ordered, Set<String> following) {

        sessions.values().removeIf(s -> !s.expires().isAfter(clock.instant()));
        if (sessions.size() >= 1000)
            sessions.remove(sessions.keySet().iterator().next());
        String id = UUID.randomUUID().toString();

        var snapshot = new Snapshot(owner, List.copyOf(ordered), Set.copyOf(following), clock.instant().plusSeconds(900));

        sessions.put(id, snapshot);

        return new Page(id, snapshot, 0);
    }

    public synchronized Page resume(String owner, String cursor) {

        if (cursor.length() > 64 || !cursor.matches("[0-9a-f-]{36}:\\d{1,3}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed cursor");

        var parts = cursor.split(":");
        var snapshot = sessions.get(parts[0]);

        if (snapshot == null || !snapshot.expires().isAfter(clock.instant()))
            throw new ResponseStatusException(HttpStatus.GONE, "Feed expired; refresh to start again");

        if (!snapshot.owner().equals(owner))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        int offset = Integer.parseInt(parts[1]);
        if (offset < 10 || offset % 10 != 0 || offset >= snapshot.ordered().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed offset");

        return new Page(parts[0], snapshot, offset);
    }
}
