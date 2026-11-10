package com.petfeed.post.repository;

import com.petfeed.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    long countByUserKeyAndDeletedFalse(String userKey);
    List<Post> findByUserKeyAndDeletedFalseOrderByIdDesc(String userKey, Pageable pageable);
    List<Post> findByUserKeyAndDeletedFalseAndIdLessThanOrderByIdDesc(String userKey, Long cursor, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> lockById(@Param("id") Long id);
}
