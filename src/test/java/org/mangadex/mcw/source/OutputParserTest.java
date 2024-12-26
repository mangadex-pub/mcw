package org.mangadex.mcw.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.mangadex.mcw.lifecycle.parse.OutputParser;
import org.mangadex.mcw.output.file.FSOutput;

@SpringBootTest(classes = {
    OutputParser.class
})
class OutputParserTest {

    @Autowired
    private OutputParser parser;

    @Test
    void parseFSOutput(@TempDir Path tempDir) {
        var path = tempDir.resolve("config.tmpl.json").toAbsolutePath();

        var uid = 1234;
        var gid = 5678;
        var output = parser.convert("file://%s?uid=%d&gid=%d".formatted(path, uid, gid));

        assertThat(output).isInstanceOfSatisfying(
            FSOutput.class, fss -> {
                assertThat(fss.path()).isEqualTo(path);
                assertThat(fss.attributes()).isNotNull();
                assertThat(fss.attributes().uid()).isEqualTo(uid);
                assertThat(fss.attributes().gid()).isEqualTo(gid);
            }
        );
    }

}
