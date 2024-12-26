package org.mangadex.mcw.lifecycle.dsn;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.util.MultiValueMapAdapter;

public record Dsn(String protocol, String value, Map<String, List<String>> parameters) {

    @JsonCreator
    public static Dsn parse(String input) {
        URI uri;
        try {
            uri = new URI(input);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid DSN string: " + input, e);
        }

        if (!uri.isAbsolute() || uri.isOpaque()) {
            throw new IllegalArgumentException("DSN string must be an absolute and non-opaque URI at: " + input);
        }

        var params = new MultiValueMapAdapter<String, String>(new HashMap<>());

        var query = uri.getQuery();
        if (query != null) {
            for (var pair : query.split("&")) {
                var kv = pair.split("=", 2);
                var k = kv[0].trim();
                var v = kv.length > 1 ? kv[1].trim() : null;
                params.add(k, v);
            }
        }

        var value = Stream.of(uri.getAuthority(), uri.getPath()).filter(Objects::nonNull).collect(Collectors.joining());

        return new Dsn(uri.getScheme(), value, params);
    }

}
