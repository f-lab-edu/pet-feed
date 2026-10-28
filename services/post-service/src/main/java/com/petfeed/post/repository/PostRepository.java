package com.petfeed.post.repository;

import com.petfeed.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    long countByUserKey(String userKey);
    List<Post> findByUserKeyOrderByIdDesc(String userKey, Pageable pageable);
    List<Post> findByUserKeyAndIdLessThanOrderByIdDesc(String userKey, Long cursor, Pageable pageable);
}
