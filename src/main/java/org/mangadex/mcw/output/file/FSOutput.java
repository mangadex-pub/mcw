package org.mangadex.mcw.output.file;

import static java.lang.Integer.parseInt;

import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.springframework.validation.annotation.Validated;

import org.mangadex.mcw.lifecycle.dsn.Dsn;
import org.mangadex.mcw.output.Output;

@Validated
public record FSOutput(
    @NotNull Path path,
    @NotNull @Validated Attributes attributes
) implements Output {

    public FSOutput {
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Path must be absolute: " + path);
        }
    }

    public static FSOutput parse(String input) {
        var dsn = Dsn.parse(input);
        return parse(dsn);
    }

    public static FSOutput parse(Dsn dsn) {
        if (!"file".equals(dsn.protocol())) {
            throw new IllegalArgumentException("Cannot parse file output for output with type '" + dsn.protocol() + "'");
        }
        var path = Paths.get(dsn.value());
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Path must be absolute: " + path);
        }

        var attributes = Attributes.DEFAULT;
        var parameters = dsn.parameters();
        for (String key : parameters.keySet()) {
            if ("uid".equals(key)) {
                attributes = new Attributes(
                    parseInt(parameters.get("uid").getFirst()),
                    attributes.gid(),
                    attributes.mode()
                );
            } else if ("gid".equals(key)) {
                attributes = new Attributes(
                    attributes.uid(),
                    parseInt(parameters.get("gid").getFirst()),
                    attributes.mode()
                );
            } else if ("mode".equals(key)) {
                attributes = new Attributes(
                    attributes.uid(),
                    attributes.gid(),
                    parseInt(parameters.get("mode").getFirst(), 8)
                );
            }
        }

        return new FSOutput(path, attributes);
    }

    public record Attributes(

        @Nullable
        @PositiveOrZero
        Integer uid,

        @Nullable
        @PositiveOrZero
        Integer gid,

        @SuppressWarnings("OctalInteger")
        @Nullable
        @Range(from = 0600, to = 0777)
        Integer mode

    ) {

        @SuppressWarnings("OctalInteger")
        public static final Attributes DEFAULT = new Attributes(
            null,
            null,
            0644
        );

        public int modeOrThrow() {
            if (mode == null) {
                throw new IllegalArgumentException("Unix file mode was not specified");
            }
            return mode;
        }

        @Override
        public String toString() {
            return "Attributes["
                   + "uid=" + (uid != null ? uid : "-") + ", "
                   + "gid=" + (gid != null ? gid : "-") + ", "
                   + "mode=" + (mode != null ? "0" + Integer.toString(mode, 8) : "-")
                   + "]";
        }

    }

}
