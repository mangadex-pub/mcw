package org.mangadex.mcw.output.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mangadex.mcw.output.file.FSOutput.Attributes;

class FSOutputTest {

    @Test
    void failsRelativePath() {
        String path = "foo/bar/baz";
        assertThatThrownBy(() -> FSOutput.parse("file://" + path))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(path);
    }

    @Test
    void failsNonFile() {
        assertThatThrownBy(() -> FSOutput.parse("nonfile://foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'nonfile'");
    }

    @Test
    void parsesWithDefaults(@TempDir Path path) {
        var parsed = FSOutput.parse("file://" + path.toAbsolutePath());
        assertThat(parsed.path()).isEqualTo(path);
        assertThat(parsed.attributes()).isEqualTo(Attributes.DEFAULT);
    }

    @SuppressWarnings("OctalInteger")
    @Test
    void parsesCustomAttributes(@TempDir Path path) {
        int uid = 123;
        int gid = 456;
        int mode = 0765;

        var parsed = FSOutput.parse("file://" + path.toAbsolutePath() + "?uid=" + uid + "&gid=" + gid + "&mode=0" + Integer.toString(mode, 8));
        assertThat(parsed.path()).isEqualTo(path);
        assertThat(parsed.attributes()).satisfies(attrs -> {
            assertThat(attrs.uid()).isEqualTo(uid);
            assertThat(attrs.gid()).isEqualTo(gid);
            assertThat(attrs.mode()).isEqualTo(mode);
        });
    }

}
