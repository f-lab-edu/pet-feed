package com.petfeed.comment.repository;

import com.petfeed.comment.domain.CommentThread;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CommentThreadRepository extends JpaRepository<CommentThread, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select thread from CommentThread thread where thread.postId = :postId")
    Optional<CommentThread> lockByPostId(@Param("postId") Long postId);
}
