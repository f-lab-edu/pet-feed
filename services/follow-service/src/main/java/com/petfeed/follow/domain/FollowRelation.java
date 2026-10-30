package com.petfeed.follow.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(name = "uk_follow_pair", columnNames = {"follower_key", "following_key"}),
        indexes = {@Index(name = "idx_follow_follower", columnList = "follower_key,id"),
                @Index(name = "idx_follow_following", columnList = "following_key,id")})
@Check(constraints = "follower_key <> following_key")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FollowRelation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "follower_key", nullable = false, updatable = false)
    private String followerKey;
    @Column(name = "following_key", nullable = false, updatable = false)
    private String followingKey;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static FollowRelation create(String follower, String following) {
        validate(follower, following);
        var relation = new FollowRelation();
        relation.followerKey = follower;
        relation.followingKey = following;
        relation.createdAt = LocalDateTime.now();
        return relation;
    }

    public static void validate(String follower, String following) {
        if (follower == null || follower.isBlank() || follower.length() > 255
                || following == null || following.isBlank() || following.length() > 255 || follower.equals(following))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Distinct valid member keys are required");
    }
}
