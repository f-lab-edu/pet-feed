package com.petfeed.post.repository;

import com.petfeed.post.domain.CommentCleanup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CommentCleanupRepository extends JpaRepository<CommentCleanup, Long> {
    List<CommentCleanup> findTop20ByNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from CommentCleanup t where t.postId = :id")
    Optional<CommentCleanup> lockById(@Param("id") Long id);
}
