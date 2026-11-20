package com.petfeed.gateway;

import com.petfeed.gateway.controller.GatewayController;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MediaProxyTest {
    @Test void bodylessMediaGetPreservesRangeAndResponseBytes() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var gateway = new GatewayController(builder.build());
        ReflectionTestUtils.setField(gateway, "facadeServiceUrl", "http://facade");
        var request = new MockHttpServletRequest("GET", "/api/v1/posts/1/media/id");
        request.addHeader("Range", "bytes=0-2");
        server.expect(requestTo("http://facade/api/v1/posts/1/media/id"))
                .andExpect(method(HttpMethod.GET)).andExpect(header("Range", "bytes=0-2"))
                .andRespond(withStatus(HttpStatus.PARTIAL_CONTENT).contentType(MediaType.parseMediaType("video/mp4"))
                        .header("Content-Range", "bytes 0-2/10").body(new byte[]{1, 2, 3}));
        var response = gateway.proxy(request, null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst("Content-Range")).isEqualTo("bytes 0-2/10");
        assertThat(response.getBody()).containsExactly(1, 2, 3);
        server.verify();
    }

    @Test void uploadPreservesBinaryBodyAndOwnerHeader() throws Exception {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var gateway = new GatewayController(builder.build());
        ReflectionTestUtils.setField(gateway, "facadeServiceUrl", "http://facade");
        var request = new MockHttpServletRequest("POST", "/api/v1/media");
        request.setContentType("image/png");
        request.addHeader("X-Member-Key", "owner");
        request.setContent(new byte[]{1, 2, 3});
        server.expect(requestTo("http://facade/api/v1/media"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("X-Member-Key", "owner"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"mediaId\":\"id\"}"));
        assertThat(gateway.upload(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        server.verify();
    }
}
