package org.mangadex.mcw.output.file;

import static java.nio.file.Files.setAttribute;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.mangadex.mcw.output.file.UnixModeUtils.toPermissions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.mangadex.mcw.output.Writer;
import org.mangadex.mcw.source.file.FSWriteMethod;

@Component
public class FSWriter implements Writer<FSOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FSWriter.class);

    private static final Map<FSOutput, FSWriteMethod> FS_WRITE_METHODS = new ConcurrentHashMap<>();

    @Override
    public void flush(FSOutput target, String rendered) throws IOException {
        LOGGER.info("Writing rendered configuration to {}", target);
        var finalPath = target.path();
        if (!Files.exists(finalPath) && !Files.exists(finalPath.getParent())) {
            throw new IOException("Output path or its parent folder must exist but did not: " + finalPath);
        }

        var writeMethod = FS_WRITE_METHODS.computeIfAbsent(target, _ -> FSWriteMethod.forPath(finalPath));

        Path tmp = tempFile(target, writeMethod);
        LOGGER.debug("Using temporary file {}", tmp.toAbsolutePath());

        Files.writeString(tmp, rendered, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LOGGER.debug("Wrote rendered configuration to temporary file {}", target);

        moveTmpToTarget(tmp, finalPath, writeMethod);
        if (target.attributes().uid() != null) {
            setAttribute(finalPath, "unix:uid", target.attributes().uid());
        }
        if (target.attributes().gid() != null) {
            setAttribute(finalPath, "unix:gid", target.attributes().gid());
        }
        if (target.attributes().mode() != null) {
            setPosixFilePermissions(finalPath, toPermissions(target.attributes().mode()));
        }
    }

    private Path tempFile(FSOutput target, FSWriteMethod method) throws IOException {
        var path = target.path();
        var id = now().toEpochSecond(UTC) + "-" + randomUUID();
        return switch (method) {
            case TMPDIR_COPY, TMPDIR_ATOMIC -> Files.createTempFile("mcw-tmpfile-" + id, ".tmp");
            case SIBLING_ATOMIC -> path.resolveSibling(path.getFileName() + ".new-" + id);
        };
    }

    private void moveTmpToTarget(Path src, Path dst, FSWriteMethod method) throws IOException {
        if (method.isAtomic()) {
            Files.move(src, dst, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
