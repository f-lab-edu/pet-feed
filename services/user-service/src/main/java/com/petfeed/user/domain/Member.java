package com.petfeed.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "members", uniqueConstraints = @UniqueConstraint(name = "uk_member_email", columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    private String userKey;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String nickname;
    private String bio;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Member(String email, String passwordHash, String phone, String nickname) {
        this.userKey = UUID.randomUUID().toString();
        this.email = email;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.nickname = nickname;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String email, String bio) {
        if (email != null) this.email = email;
        if (bio != null) this.bio = bio;
    }
}
