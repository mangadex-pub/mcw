package org.mangadex.mcw.source.file;

import static java.nio.file.Files.newInputStream;

import java.io.BufferedInputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;

class FSWatch extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(FSWatch.class);
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final FSSource source;
    private final Consumer<String> callback;

    private boolean running = false;

    FSWatch(FSSource source, Consumer<String> callback) {
        this.source = source;
        this.callback = callback;
        setName("watch-" + COUNTER.getAndIncrement());
        setUncaughtExceptionHandler((_, e) -> LOGGER.error("Uncaught exception", e));
    }

    @Override
    public void start() {
        running = true;
        super.start();
    }

    @Override
    public void run() {
        LOGGER.debug("Started FSWatch for {}", source);
        String lastChecksum = null;
        while (running) {
            try (var is = new BufferedInputStream(newInputStream(source.path()))) {
                var bytes = is.readAllBytes();
                if (bytes.length == 0) {
                    continue;
                }

                var checksum = DigestUtils.md5DigestAsHex(bytes);
                if (!Objects.equals(lastChecksum, checksum)) {
                    LOGGER.info("Template file changed (md5: {} -> {})", lastChecksum, checksum);
                    lastChecksum = checksum;
                    callback.accept(new String(bytes));
                } else {
                    LOGGER.debug("Template file unchanged (md5: {})", checksum);
                }
            } catch (Throwable e) {
                LOGGER.error("Unable to read template file: {} {}", e.getClass().getSimpleName(), e.getMessage());
            }

            try {
                Thread.sleep(source.period());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.debug("Stopped FSWatch for {}", source);
    }

    public void shutdown() {
        running = false;
    }

    public boolean isShuttingDown() {
        return !running;
    }

}
