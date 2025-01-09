package org.mangadex.mcw;

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import org.mangadex.mcw.lifecycle.MCRConfigCollector;
import org.mangadex.mcw.lifecycle.MCRWatchLifecycler;

@Component
public class Bootstrap implements ApplicationRunner {

    private final MCRConfigCollector configCollector;
    private final MCRWatchLifecycler watchLifecycler;

    public Bootstrap(MCRConfigCollector configCollector, MCRWatchLifecycler watchLifecycler) {
        this.configCollector = configCollector;
        this.watchLifecycler = watchLifecycler;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        configCollector.findAll(args).forEach(watchLifecycler::register);
    }

    public record BuildInfo(
        String version,
        String timestamp
    ) {

        public static BuildInfo fromApplicationYaml() {
            try {
                var properties = new YamlPropertySourceLoader().load("meta", new ClassPathResource("application.yml")).getFirst();
                var version = requireNonNull((String) properties.getProperty("spring.application.version"));
                var timestamp = requireNonNull((String) properties.getProperty("spring.application.build.timestamp"));
                return new BuildInfo(version, timestamp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
