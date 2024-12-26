package org.mangadex.mcw.output.file;

import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mangadex.mcw.output.file.UnixModeUtils.toPermissions;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.mangadex.mcw.output.file.FSOutput.Attributes;

@SpringBootTest(classes = FSWriter.class)
class FSWriterTest {

    @Autowired
    private FSWriter fsWriter;

    @Test
    void writeFileDefault(@TempDir Path tempDir) throws IOException {
        var content = "Hello World!";
        var target = tempDir.resolve("output");
        var output = new FSOutput(target, Attributes.DEFAULT);

        fsWriter.flush(output, content);

        assertThat(target).isRegularFile().hasContent(content);
        assertThat(Files.getPosixFilePermissions(target)).isEqualTo(toPermissions(output.attributes().modeOrThrow()));
    }

    @Test
    void writeFileWithCustomAttributes(@TempDir Path tempDir) throws IOException {
        var content = "Hello World!";
        var target = tempDir.resolve("output");

        var selfuid = parseInt(new String(getRuntime().exec(new String[]{"id", "-u"}).getInputStream().readAllBytes()).trim());
        var selfgid = parseInt(new String(getRuntime().exec(new String[]{"id", "-g"}).getInputStream().readAllBytes()).trim());
        var mode = Attributes.DEFAULT.modeOrThrow();

        // cannot well test different owner, as we do not change permissions atomically from one another,
        // so just after setting the new owner, we might very well have lost the ability to change the group/mode!
        var output = new FSOutput(target, new Attributes(selfuid, selfgid + 1, mode));
        fsWriter.flush(output, content);

        assertThat(target).isRegularFile().hasContent(content);
        assertThat(Files.getAttribute(target, "unix:uid")).isEqualTo(output.attributes().uid());
        assertThat(Files.getAttribute(target, "unix:gid")).isEqualTo(output.attributes().gid());
        assertThat(Files.getPosixFilePermissions(target)).isEqualTo(toPermissions(output.attributes().modeOrThrow()));
    }

    @Test
    void failsOnNonExistentPath() {
        var output = new FSOutput(Path.of("/does/not/exist"), Attributes.DEFAULT);
        assertThatThrownBy(() -> fsWriter.flush(output, "Hello World!"))
            .isInstanceOf(IOException.class)
            .hasMessageContaining(output.path().toAbsolutePath().toString());
    }

    @Test
    void failsOnMissingPermissions(@TempDir Path tempDir) throws IOException {
        var outdir = Files.createDirectory(tempDir.resolve("output-wrapped"));
        Files.setPosixFilePermissions(outdir, PosixFilePermissions.fromString("r--r--r--"));
        var output = new FSOutput(outdir.resolve("output"), Attributes.DEFAULT);

        assertThatThrownBy(() -> fsWriter.flush(output, "Hello World!"))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining(output.path().toAbsolutePath().toString());
    }

}
