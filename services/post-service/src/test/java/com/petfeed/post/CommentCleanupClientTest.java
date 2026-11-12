package com.petfeed.post;

import com.petfeed.post.service.CommentCleanupClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;

class CommentCleanupClientTest {
    @Test void sendsInternalDeleteAndPropagatesFailureForRetry() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var calls = new AtomicInteger();
        server.createContext("/internal/v1/posts/42/comments", exchange -> {
            if (!exchange.getRequestMethod().equals("DELETE")) {
                exchange.sendResponseHeaders(405, -1);
            } else {
                exchange.sendResponseHeaders(calls.incrementAndGet() == 1 ? 503 : 204, -1);
            }
            exchange.close();
        });
        server.start();
        try {
            var client = new CommentCleanupClient(RestClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort());
            assertThatThrownBy(() -> client.close(42L)).isInstanceOf(RestClientResponseException.class);
            client.close(42L);
            assertThat(calls.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }
}
