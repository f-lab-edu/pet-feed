package com.petfeed.post.repository;

import com.petfeed.post.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndPostId(Long id, Long postId);
    List<Comment> findByPostIdOrderByIdDesc(Long postId, Pageable page);
    List<Comment> findByPostIdAndIdLessThanOrderByIdDesc(Long postId, Long cursor, Pageable page);
    void deleteByPostId(Long postId);
    int countByPostId(Long postId);
}
