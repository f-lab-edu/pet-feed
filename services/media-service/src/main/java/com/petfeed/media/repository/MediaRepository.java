package com.petfeed.media.repository;

import com.petfeed.media.domain.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaFile, String> {}
