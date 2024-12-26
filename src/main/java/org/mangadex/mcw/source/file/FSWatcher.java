package org.mangadex.mcw.source.file;

import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.mangadex.mcw.source.Watcher;

@Component
public class FSWatcher implements Watcher<FSSource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FSWatcher.class);

    private static final Map<FSSource, FSWatch> WATCHES = new ConcurrentHashMap<>();
    private static final ReentrantLock LOCK = new ReentrantLock();

    @Override
    public void addWatch(FSSource source, Consumer<String> onChanged) {
        try {
            LOCK.lock();

            if (!isRegularFile(source.path())) {
                LOGGER.warn("Source path not found or is not a file: {}", source.path());
            } else if (!isReadable(source.path())) {
                LOGGER.warn("Source path is not readable: {}", source.path());
            }

            if (WATCHES.containsKey(source)) {
                LOGGER.warn("File watch already exists on {}", source);
            } else {
                LOGGER.debug("Registered file watch on {}", source);
                FSWatch watch = new FSWatch(source, onChanged);
                WATCHES.put(source, watch);
                watch.start();
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to register file watch on {}", source, e);
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void removeWatch(FSSource source) {
        try {
            LOCK.lock();

            if (!WATCHES.containsKey(source)) {
                LOGGER.debug("No existing file watch on {}", source);
                return;
            }

            FSWatch watch = WATCHES.remove(source);
            if (watch.isShuttingDown()) {
                LOGGER.warn("Watch on {} is already shutting down", source);
            } else {
                watch.shutdown();
                LOGGER.debug("Shutting down file watch on {}", source);
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to unregister file watch on {}", source, e);
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public Set<FSSource> getSources() {
        return WATCHES.keySet();
    }

}
