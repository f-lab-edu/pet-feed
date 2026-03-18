package com.petfeed.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

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

    @RequestMapping("/**")
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

        return restClient.method(method)
                .uri(targetUrl)
                .headers(h -> h.addAll(headers))
                .body(body)
                .retrieve()
                .toEntity(byte[].class);
    }
}
