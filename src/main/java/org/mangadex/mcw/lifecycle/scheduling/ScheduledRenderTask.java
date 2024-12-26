package org.mangadex.mcw.lifecycle.scheduling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.util.function.ThrowingFunction;

import org.mangadex.mcw.render.Render;

public final class ScheduledRenderTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledRenderTask.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<ScheduledFuture<?>> nextTick = new AtomicReference<>();

    private final ThrowingFunction<String, Render> render;
    private final ThrowingConsumer<String> write;
    private final long retryDelaySeconds;

    public ScheduledRenderTask(
        ThrowingFunction<String, Render> render,
        ThrowingConsumer<String> write,
        long retryDelaySeconds
    ) {
        this.render = render;
        this.write = write;
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public void stop() {
        executor.shutdownNow();
    }

    public void templateChanged(String template) {
        if (nextTick.get() != null) {
            nextTick.get().cancel(true);
        }

        renderWriteAndSchedule(template);
    }

    private void renderWriteAndSchedule(String template) {
        if (executor.isShutdown()) {
            LOGGER.warn("Render task is shutting down, ignoring template change...");
            return;
        }

        long nextScheduleSeconds;
        try {
            Render render = this.render.applyWithException(template);
            write.accept(render.rendered());
            nextScheduleSeconds = render.ttl();
        } catch (Exception e) {
            LOGGER.error("Failed rendering template", e);
            nextScheduleSeconds = retryDelaySeconds;
        }

        nextTick.set(executor.schedule(() -> renderWriteAndSchedule(template), nextScheduleSeconds, TimeUnit.SECONDS));
    }

}
