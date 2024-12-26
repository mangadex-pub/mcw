package org.mangadex.mcw.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.mangadex.mcw.lifecycle.parse.SourceParser;
import org.mangadex.mcw.source.file.FSSource;

@SpringBootTest(classes = {
    SourceConfiguration.class,
    SourceParser.class
})
class SourceParserTest {

    @Autowired
    private SourceParser parser;

    @Test
    void parseFSSource(@TempDir Path tempDir) {
        var path = tempDir.resolve("config.tmpl.json").toAbsolutePath();
        var pollI = Duration.ofSeconds(10);

        var source = parser.convert("file://%s?period=%s".formatted(path, pollI.toMillis()));

        assertThat(source).isInstanceOfSatisfying(
            FSSource.class, fss -> {
                assertThat(fss.path()).isEqualTo(path);
                assertThat(fss.period()).isEqualTo(pollI);
            }
        );
    }

}
