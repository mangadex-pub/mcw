package org.mangadex.mcw.source.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FSSourceTest {

    @Test
    void failsRelativePath() {
        String path = "foo/bar/baz";
        assertThatThrownBy(() -> FSSource.parse("file://" + path, Duration.ofSeconds(1L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(path);
    }

    @Test
    void failsNonFile() {
        assertThatThrownBy(() -> FSSource.parse("nonfile://foo", Duration.ofSeconds(1L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'nonfile'");
    }

    @Test
    void parsesWithDefaultPeriod(@TempDir Path path) {
        var defaultPeriod = Duration.ofHours(24L);
        var parsed = FSSource.parse("file://" + path.toAbsolutePath(), defaultPeriod);
        assertThat(parsed.path()).isEqualTo(path);
        assertThat(parsed.period()).isEqualTo(defaultPeriod);
    }

    @Test
    void parsesWithHumanPeriod(@TempDir Path path) {
        var parsed = FSSource.parse("file://" + path.toAbsolutePath() + "?period=42s", Duration.ofHours(24L));
        assertThat(parsed.path()).isEqualTo(path);
        assertThat(parsed.period()).isEqualTo(Duration.ofSeconds(42L));
    }

    @Test
    void parsesWithISOPeriod(@TempDir Path path) {
        var parsed = FSSource.parse("file://" + path.toAbsolutePath() + "?period=PT42s", Duration.ofSeconds(42L));
        assertThat(parsed.path()).isEqualTo(path);
        assertThat(parsed.period()).isEqualTo(Duration.ofSeconds(42L));
    }

}
