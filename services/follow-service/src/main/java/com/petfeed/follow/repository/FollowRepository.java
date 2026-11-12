package com.petfeed.follow.repository;

import com.petfeed.follow.domain.FollowRelation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface FollowRepository extends JpaRepository<FollowRelation, Long> {
    boolean existsByFollowerKeyAndFollowingKey(String follower, String following);
    void deleteByFollowerKeyAndFollowingKey(String follower, String following);

    @Query("select f from FollowRelation f where f.followerKey = :key and (:cursor = 0 or f.id < :cursor) order by f.id desc")
    List<FollowRelation> followings(@Param("key") String key, @Param("cursor") Long cursor, Pageable page);
    @Query("select f from FollowRelation f where f.followingKey = :key and (:cursor = 0 or f.id < :cursor) order by f.id desc")
    List<FollowRelation> followers(@Param("key") String key, @Param("cursor") Long cursor, Pageable page);
    @Query("select f.followingKey from FollowRelation f where f.followerKey = :key and f.followingKey in :targets")
    Set<String> targets(@Param("key") String key, @Param("targets") List<String> targets);
}
