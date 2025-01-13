package org.mangadex.mcw.source.file;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum FSWriteMethod {

    TMPDIR_ATOMIC(true),
    TMPDIR_COPY(false),
    SIBLING_ATOMIC(true);

    private static final Logger LOGGER = LoggerFactory.getLogger(FSWriteMethod.class);

    private static final FileStore TMPDIR_FILESTORE;

    static {
        try {
            TMPDIR_FILESTORE = Files.getFileStore(Paths.get(System.getProperty("java.io.tmpdir")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final boolean atomic;

    FSWriteMethod(boolean atomic) {
        this.atomic = atomic;
    }

    public boolean isAtomic() {
        return atomic;
    }

    public static FSWriteMethod forPath(Path path) {
        if (!Files.exists(path)) {
            path = path.getParent();
        }

        try {
            path = path.toAbsolutePath().toRealPath();
        } catch (IOException e) {
            LOGGER.warn("Cannot determine real absolute path of {}", path, e);
        }

        var parent = path;
        if (!Files.isDirectory(parent)) {
            parent = parent.getParent();
        }

        FileStore fstore;
        try {
            fstore = Files.getFileStore(parent);
        } catch (IOException e) {
            LOGGER.warn("Cannot determine backing filesystem of path parent {}, using tmpdir non-atomic copy write method", parent, e);
            return FSWriteMethod.TMPDIR_COPY;
        }

        if (Objects.equals(fstore, TMPDIR_FILESTORE)) {
            LOGGER.trace("tmpdir and path parent {} have the same backing filesystem, using tmpdir atomic move write method", path);
            return FSWriteMethod.TMPDIR_ATOMIC;
        }

        LOGGER.trace("tmpdir and path parent {} have different backing filesystem, using sibling atomic move write method", path);
        return FSWriteMethod.SIBLING_ATOMIC;
    }

}
