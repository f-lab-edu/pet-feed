package com.petfeed.gateway.controller;

import com.petfeed.gateway.exception.DownstreamException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class GatewayController {

    private static final Set<String> EXCLUDED_HEADERS
            = Set.of(
            "host", // 호스트 변질 방지
            "content-length",  // body 길이가 다를 수 있음.
            "transfer-encoding"  // 스트림 소비 시 인코딩방식.
    );

    private final RestClient restClient;

    @Value("${services.facade-service.url}")
    private String facadeServiceUrl;

    @PostMapping("/api/v1/media")
    public ResponseEntity<byte[]> upload(HttpServletRequest request) throws IOException {
        byte[] body = request.getInputStream().readNBytes(50 * 1024 * 1024 + 1);
        if (body.length > 50 * 1024 * 1024)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        return proxy(request, body);
    }

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String targetUrl = facadeServiceUrl + request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString != null) {
            targetUrl += "?" + queryString;
        }

        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames())
                .stream()
                .filter(name -> !EXCLUDED_HEADERS.contains(name.toLowerCase()))
                .forEach(name -> headers.set(name, request.getHeader(name)));

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        var outbound = restClient.method(method)
                .uri(targetUrl)
                .headers(h -> h.addAll(headers));
        if (body != null) outbound.body(body);
        return outbound.retrieve()
                .onStatus(HttpStatusCode::isError, this::propagateDownstreamError)
                .toEntity(byte[].class);
    }

    private void propagateDownstreamError(HttpRequest httpRequest, ClientHttpResponse clientHttpResponse) throws IOException {
        byte[] body;
        try {
            body = clientHttpResponse.getBody().readAllBytes();
        } catch (IOException e) {
            body = new byte[0];
        }
        throw new DownstreamException(clientHttpResponse.getStatusCode().value(), body);
    }

}
