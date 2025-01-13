package org.mangadex.mcw.render;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.DigestUtils.md5DigestAsHex;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.validation.annotation.Validated;

@Validated
public record Render(
    @NotNull String rendered,
    @PositiveOrZero long ttl,
    @NotNull String md5sum
) {

    public Render(
        @NotNull String rendered,
        @PositiveOrZero long ttl
    ) {
        this(rendered, ttl, md5DigestAsHex(rendered.getBytes(UTF_8)));
    }

}
