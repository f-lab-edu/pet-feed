package com.petfeed.media;

import com.petfeed.media.service.MediaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MediaRestartTest {
    @TempDir Path directory;

    @Test
    void uploadedBytesAndOwnershipSurviveApplicationRestart() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", bytes);
        String id;
        try (var context = start()) {
            id = context.getBean(MediaService.class).upload("owner", "image/png", bytes.toByteArray()).mediaId();
        }
        try (var context = start()) {
            var service = context.getBean(MediaService.class);
            assertThat(service.validateOwnership("owner", List.of(id))).hasSize(1);
            var mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
            mvc.perform(get("/internal/v1/media/{id}/content", id))
                    .andExpect(status().isOk()).andExpect(content().bytes(bytes.toByteArray()));
            mvc.perform(get("/internal/v1/media/{id}/content", id).header("Range", "bytes=0-7"))
                    .andExpect(status().isPartialContent())
                    .andExpect(content().bytes(java.util.Arrays.copyOf(bytes.toByteArray(), 8)));
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(MediaServiceApplication.class).run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:file:" + directory.resolve("media").toString().replace('\\', '/') + ";DB_CLOSE_ON_EXIT=FALSE",
                "--media.storage-path=" + directory.resolve("files"));
    }
}
