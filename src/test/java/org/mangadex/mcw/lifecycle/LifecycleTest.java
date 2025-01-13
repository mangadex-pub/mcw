package org.mangadex.mcw.lifecycle;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

import org.mangadex.mcw.dns.KnotSidecar;
import org.mangadex.mcw.output.file.FSOutput;
import org.mangadex.mcw.output.file.FSOutput.Attributes;
import org.mangadex.mcw.source.file.FSSource;

@SpringBootTest(properties = {
    // force 1s file check period
    "org.mangadex.mcw.source.file.check-period-seconds=1",
    // do not deal with render result TTL here
    "org.mangadex.mcw.render.ttl.min=86400",
    "org.mangadex.mcw.render.ttl.max=86400",
})
@ImportTestcontainers(KnotSidecar.class)
class LifecycleTest {

    @Autowired
    private MCRWatchRegistry lifecycler;

    @Test
    void registerAndEditTemplate(@TempDir Path tempDir) throws IOException, JSONException {
        Path source = tempDir.resolve("config.template.json");
        Path output = tempDir.resolve("config.rendered.json");

        lifecycler.register(new MCRConfig(
            new FSSource(source, Duration.ofSeconds(1L)),
            new FSOutput(output, Attributes.DEFAULT)
        ));

        // ie no empty-rendering
        assertThat(output).doesNotExist();

        @Language("JSON5")
        String template1 = """
            {
              "pools": {
                "test": {
                  "servers": [
                    "dnssrv://_memcache._tcp.memcache-srv.mcw.mangadex"
                  ]
                }
              }
            }
            """;
        Files.writeString(source, template1, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        await().atMost(3, SECONDS).until(() -> Files.exists(output));

        @Language("JSON")
        String expected1 = """
            {
              "pools": {
                "test": {
                  "servers": [
                    "10.0.0.1:11211",
                    "10.0.0.2:11211",
                    "10.0.0.3:11211"
                  ]
                }
              }
            }
            """;
        var actual1 = Files.readString(output);
        var lastModified = Files.getLastModifiedTime(output).toMillis();
        assertEquals("Content:\n" + actual1, expected1, actual1, true);

        @Language("JSON5")
        String template2 = """
            {
              "pools": {
                "test": {
                  "servers": [
                    "dns+1234://memcache-static-vip.mcw.mangadex"
                  ]
                }
              }
            }
            """;
        Files.writeString(source, template2, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        await().atMost(3, SECONDS).until(() -> Files.getLastModifiedTime(output).toMillis() > lastModified);

        @Language("JSON")
        String expected2 = """
            {
              "pools": {
                "test": {
                  "servers": [
                    "10.0.0.0:1234"
                  ]
                }
              }
            }
            """;
        var actual2 = Files.readString(output);
        assertEquals("Content:\n" + actual2, expected2, actual2, true);

        lifecycler.stop();
    }

}
