package com.petfeed.comment.repository;

import com.petfeed.comment.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByIdAndPostId(Long id, Long postId);
    List<Comment> findByPostIdOrderByIdDesc(Long postId, Pageable page);
    List<Comment> findByPostIdAndIdLessThanOrderByIdDesc(Long postId, Long cursor, Pageable page);
    void deleteByPostId(Long postId);
    int countByPostId(Long postId);
    interface PostCount {
        Long getPostId();
        Long getTotal();
    }
    @Query("select c.postId as postId, count(c) as total from Comment c where c.postId in :postIds group by c.postId")
    List<PostCount> countForPosts(@Param("postIds") List<Long> postIds);
}
