package com.petfeed.media;

import com.petfeed.media.repository.MediaRepository;
import com.petfeed.media.service.MediaValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MediaIntegrationTest {
    @TempDir static Path storage;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("media.storage-path", storage::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MediaRepository files;

    byte[] image(String format) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), format, output);
        return output.toByteArray();
    }

    String upload(byte[] body, String type) throws Exception {
        var response = mvc.perform(post("/internal/v1/media").header("X-Member-Key", "owner")
                        .contentType(type).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value(type)).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).get("mediaId").asText();
    }

    @Test void photosPersistAndOnlyOwnerCanAttachThem() throws Exception {
        for (String format : new String[]{"png", "jpeg"}) {
            byte[] data = image(format);
            String id = upload(data, "image/" + format);
            mvc.perform(get("/internal/v1/media/{id}/content", id)).andExpect(status().isOk())
                    .andExpect(content().bytes(data)).andExpect(header().string("Cache-Control", "no-store"));
            mvc.perform(post("/internal/v1/media/validate").header("X-Member-Key", "owner")
                    .contentType("application/json").content("[\"" + id + "\"]"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].mediaId").value(id));
            mvc.perform(post("/internal/v1/media/validate").header("X-Member-Key", "other")
                    .contentType("application/json").content("[\"" + id + "\"]"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void rejectsSpoofedEmptyOversizedAndMissingUploads() throws Exception {
        long before = files.count();
        mvc.perform(post("/internal/v1/media").header("X-Member-Key", "owner")
                .contentType("image/png").content("<script>alert(1)</script>"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/internal/v1/media").header("X-Member-Key", "owner")
                .contentType("image/jpeg").content(image("png"))).andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/internal/v1/media").header("X-Member-Key", "owner")
                .contentType("video/mp4").content(new byte[0])).andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/internal/v1/media").header("X-Member-Key", "owner")
                .contentType("image/png").content(new byte[10 * 1024 * 1024 + 1])).andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/internal/v1/media").contentType("image/png").content(image("png")))
                .andExpect(status().isBadRequest());
        assertThat(files.count()).isEqualTo(before);
    }

    @Test void videoContainerIsServedWithRangeSupport() throws Exception {
        // Structural fixture: codec playback is verified separately with an actual video.
        byte[] data = ByteBuffer.allocate(34).putInt(16).put("ftypisom".getBytes(StandardCharsets.US_ASCII))
                .putInt(0).putInt(9).put("moov".getBytes(StandardCharsets.US_ASCII)).put((byte) 0)
                .putInt(9).put("mdat".getBytes(StandardCharsets.US_ASCII)).put((byte) 0).array();
        String id = upload(data, "video/mp4");
        mvc.perform(get("/internal/v1/media/{id}/content", id).header("Range", "bytes=0-7"))
                .andExpect(status().isPartialContent()).andExpect(header().string("Content-Range", "bytes 0-7/34"))
                .andExpect(content().bytes(java.util.Arrays.copyOf(data, 8)));
        mvc.perform(get("/internal/v1/media/{id}/content", id).header("Range", "bytes=100-200"))
                .andExpect(status().isRequestedRangeNotSatisfiable());
        assertThatThrownBy(() -> MediaValidator.validate(java.util.Arrays.copyOf(data, 20), "video/mp4"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
