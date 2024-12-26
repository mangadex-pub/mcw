package org.mangadex.mcw.source.file;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = FSWatcher.class)
class FSWatcherTest {

    @Autowired
    private FSWatcher watcher;

    @TempDir
    private Path dir;

    @Test
    public void watchesNotYetPresentFile() {
        FSSource source = new FSSource(dir.resolve(UUID.randomUUID().toString()), Duration.ofSeconds(5));
        watcher.addWatch(
            source, _ -> {
            }
        );
        watcher.removeWatch(source);
    }

    @Test
    public void addWatchFile() throws IOException {
        var watched = Files.createTempFile(dir, "test-", ".conf");
        var watchSource = new FSSource(watched.toAbsolutePath(), Duration.ofMillis(100L));

        List<String> templatesSeen = new ArrayList<>();
        watcher.addWatch(watchSource, templatesSeen::add);

        String v1 = "Conf1=" + UUID.randomUUID();
        Files.writeString(watched, v1, TRUNCATE_EXISTING);
        await().pollInterval(100, MILLISECONDS).atMost(3, SECONDS).until(() -> templatesSeen.size() == 1L);

        String v2 = "Conf2=" + UUID.randomUUID();
        Files.writeString(watched, v2, TRUNCATE_EXISTING);
        await().pollInterval(100, MILLISECONDS).atMost(3, SECONDS).until(() -> templatesSeen.size() == 2L);

        String v3 = "Conf3=" + UUID.randomUUID();
        Files.delete(watched);
        Files.writeString(watched, v3, CREATE_NEW);
        await().pollInterval(100, MILLISECONDS).atMost(3, SECONDS).until(() -> templatesSeen.size() == 3L);

        assertThat(templatesSeen).containsExactly(v1, v2, v3);

        watcher.removeWatch(watchSource);
    }

}
