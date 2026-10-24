package com.petfeed.post;

import com.petfeed.post.domain.Post;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class PostTest {
    @Test void authorIsRetainedAcrossUpdates() {
        var post = Post.create("owner", "before", "image");
        var createdAt = post.getCreatedAt();
        post.update("owner", "after", null);
        assertThat(post.getUserKey()).isEqualTo("owner");
        assertThat(post.getCreatedAt()).isEqualTo(createdAt);
        assertThat(post.getContent()).isEqualTo("after");
        assertThat(post.getThumbnailUrl()).isNull();
    }

    @Test void unauthorizedChangeDoesNotMutateContent() {
        var post = Post.create("owner", "before", null);
        assertThatThrownBy(() -> post.update("other", "after", null))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(post.getContent()).isEqualTo("before");
    }

    @Test void invalidContentCannotCreatePost() {
        for (String content : new String[]{null, " ", "x".repeat(5001)}) {
            assertThatThrownBy(() -> Post.create("owner", content, null))
                    .isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> Post.create(" ", "hello", null))
                .isInstanceOf(ResponseStatusException.class);
    }
}
