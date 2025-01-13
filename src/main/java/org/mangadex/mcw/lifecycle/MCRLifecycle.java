package org.mangadex.mcw.lifecycle;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class MCRLifecycle implements SmartLifecycle {

    private final ApplicationArguments applicationArguments;
    private final MCRConfigCollector configCollector;
    private final MCRWatchRegistry watchLifecycler;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public MCRLifecycle(
        ApplicationArguments applicationArguments,
        MCRConfigCollector configCollector,
        MCRWatchRegistry watchLifecycler
    ) {
        this.applicationArguments = applicationArguments;
        this.configCollector = configCollector;
        this.watchLifecycler = watchLifecycler;
    }

    @Override
    public void start() {
        watchLifecycler.start();
        configCollector.findAll(applicationArguments).forEach(watchLifecycler::register);
        running.set(true);
    }

    @Override
    public void stop() {
        running.set(false);
        watchLifecycler.stop();
    }

    @Override
    public boolean isRunning() {
        return running.get();
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
