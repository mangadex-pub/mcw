package org.mangadex.mcw.lifecycle;

import static java.util.Collections.unmodifiableSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingConsumer;

import org.mangadex.mcw.lifecycle.scheduling.ScheduledRenderTask;
import org.mangadex.mcw.output.file.FSOutput;
import org.mangadex.mcw.output.file.FSWriter;
import org.mangadex.mcw.render.RenderService;
import org.mangadex.mcw.source.file.FSSource;
import org.mangadex.mcw.source.file.FSWatcher;

@Component
public class MCRWatchLifecycler implements SmartLifecycle, InfoContributor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCRWatchLifecycler.class);

    private final ApplicationContext context;
    private final RenderService renderService;
    private final LifecycleProperties lifecycleProperties;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<MCRConfig, Registration> registrations = new ConcurrentHashMap<>();
    private final Set<MCRConfig> registeredConfigs = unmodifiableSet(registrations.keySet());

    public MCRWatchLifecycler(ApplicationContext context, RenderService renderService, LifecycleProperties lifecycleProperties) {
        this.context = context;
        this.renderService = renderService;
        this.lifecycleProperties = lifecycleProperties;
    }

    public void register(MCRConfig config) {
        if (!running.get()) {
            throw new IllegalStateException("Application is not running yet. Ignoring registration of " + config);
        }

        LOGGER.info("Registering config watch for {}", config);

        var source = config.source();
        var output = config.output();

        ThrowingConsumer<String> writeToOutput;
        switch (output) {
            case FSOutput fso -> {
                var fswriter = context.getBean(FSWriter.class);
                writeToOutput = content -> fswriter.flush(fso, content);
            }
            default -> throw new UnsupportedOperationException("Unsupported output type " + output.getClass().getSimpleName());
        }

        ScheduledRenderTask renderTask = new ScheduledRenderTask(
            renderService::render,
            writeToOutput,
            lifecycleProperties.retryDelaySeconds()
        );

        switch (source) {
            case FSSource fss -> {
                var watcher = context.getBean(FSWatcher.class);
                watcher.addWatch(fss, renderTask::templateChanged);
                registrations.put(
                    config,
                    new Registration(() -> watcher.removeWatch(fss), renderTask)
                );
            }
            default -> throw new UnsupportedOperationException("Unsupported source type " + source.getClass().getSimpleName());
        }

        LOGGER.info("Started config watch for {}", config);
    }

    @Override
    public void start() {
        LOGGER.info("Starting config watches");
        running.set(true);
    }

    @Override
    public void stop() {
        LOGGER.info("Shutting down all config watches");
        running.set(false);
        registrations.forEach((config, registration) -> {
            LOGGER.info("Stopping config watch for {}", config);
            try {
                registration.stopWatching().run();
                registration.renderTask.stop();
            } catch (Throwable t) {
                LOGGER.error("Error while stopping config watch for {}", config, t);
            }
        });
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void contribute(Builder builder) {
        builder.withDetail("registrations", registeredConfigs);
    }

    private record Registration(
        Runnable stopWatching,
        ScheduledRenderTask renderTask
    ) { }

}
