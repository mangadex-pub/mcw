package org.mangadex.mcw.render;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.jetbrains.annotations.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Templating settings
 *
 * @param defaultPort default port for DNS A based resolution if not specified (ie dns://host form, rather than dns+port://host)
 */
@Validated
@ConfigurationProperties(prefix = "org.mangadex.mcw.render")
public record RenderProperties(
    @Range(from = 1, to = 65535) int defaultPort,
    @NotNull @Validated RenderTTL ttl
) {

    @Validated
    public record RenderTTL(
        @Positive long min,
        @Positive long max
    ) { }

}
