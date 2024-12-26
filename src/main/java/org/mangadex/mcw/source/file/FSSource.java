package org.mangadex.mcw.source.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.springframework.boot.convert.DurationStyle;

import org.mangadex.mcw.lifecycle.dsn.Dsn;
import org.mangadex.mcw.source.Source;

public record FSSource(Path path, Duration period) implements Source {

    public static final String FS_SOURCE_OPTION_PERIOD = "period";

    public FSSource {
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Path must be absolute: " + path);
        }
    }

    public static FSSource parse(String input, Duration defaultPeriod) {
        var dsn = Dsn.parse(input);
        return parse(dsn, defaultPeriod);
    }

    public static FSSource parse(Dsn dsn, Duration defaultPeriod) {
        if (!"file".equals(dsn.protocol())) {
            throw new IllegalArgumentException("Cannot parse input with protocol '" + dsn.protocol() + "' as file source");
        }

        var path = Paths.get(dsn.value());
        var period = dsn.parameters().containsKey(FS_SOURCE_OPTION_PERIOD)
            ? DurationStyle.detectAndParse(dsn.parameters().get(FS_SOURCE_OPTION_PERIOD).getFirst())
            : defaultPeriod;
        return new FSSource(path, period);
    }

    @Override
    public String toString() {
        return "file://" + path.toAbsolutePath();
    }

}
