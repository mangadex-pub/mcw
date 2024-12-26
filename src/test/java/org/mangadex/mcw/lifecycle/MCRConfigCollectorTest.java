package org.mangadex.mcw.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.mangadex.mcw.lifecycle.parse.MCRConfigParser;
import org.mangadex.mcw.lifecycle.parse.OutputParser;
import org.mangadex.mcw.lifecycle.parse.SourceParser;
import org.mangadex.mcw.output.file.FSOutput;
import org.mangadex.mcw.output.file.FSOutput.Attributes;
import org.mangadex.mcw.source.SourceConfiguration;
import org.mangadex.mcw.source.SourceProperties;
import org.mangadex.mcw.source.file.FSSource;

@SpringBootTest(classes = {
    LifecycleConfiguration.class,
    SourceConfiguration.class,
    MCRConfigCollector.class,
    MCRConfigParser.class,
    SourceParser.class,
    OutputParser.class,
})
@ActiveProfiles("test-config-watches")
class MCRConfigCollectorTest {

    @Autowired
    private MCRConfigCollector collector;

    @Autowired
    private MCRConfigProperties configsProperties;

    @Autowired
    private SourceProperties sourceProperties;

    @Autowired
    private MCRConfigParser configParser;

    @Test
    void collectWatchesFromArgs(@TempDir Path tempDir) {
        var source = tempDir.resolve("source");
        var output = tempDir.resolve("output");

        var configs = collector.findAll(new DefaultApplicationArguments(
            "--source=file://" + source.toAbsolutePath(),
            "--output=file://" + output.toAbsolutePath()
        ));

        var argumentsWatch = new MCRConfig(
            new FSSource(source, Duration.ofSeconds(sourceProperties.file().checkPeriodSeconds())),
            new FSOutput(output, Attributes.DEFAULT)
        );

        var configWatches = configsProperties
            .configs()
            .stream()
            .map(wp -> configParser.parseWatch(wp.source(), wp.output()))
            .toList();

        var expected = new ArrayList<>();
        expected.add(argumentsWatch);
        expected.addAll(configWatches);

        assertThat(configs).isEqualTo(expected);
    }

}
