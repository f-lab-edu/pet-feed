package com.petfeed.post.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CommentCleanupClient {
    private final RestClient client;

    public CommentCleanupClient(RestClient.Builder builder,
            @Value("${comment-service.url:http://localhost:8084}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = builder.baseUrl(url).requestFactory(factory).build();
    }

    public void close(Long postId) {
        client.delete().uri("/internal/v1/posts/{id}/comments", postId)
                .retrieve().toBodilessEntity();
    }
}
