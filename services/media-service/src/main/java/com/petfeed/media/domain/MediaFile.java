package com.petfeed.media.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "media_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MediaFile {
    @Id
    private String id;
    @Column(nullable = false, updatable = false)
    private String ownerKey;
    @Column(nullable = false, updatable = false)
    private String contentType;
    @Column(nullable = false, updatable = false)
    private long size;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public MediaFile(String id, String ownerKey, String contentType, long size) {
        this.id = id;
        this.ownerKey = ownerKey;
        this.contentType = contentType;
        this.size = size;
        this.createdAt = Instant.now();
    }
}
