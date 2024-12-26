package org.mangadex.mcw.source;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import org.mangadex.mcw.source.file.FSSourceSettings;

@Validated
@ConfigurationProperties(prefix = "org.mangadex.mcw.source")
public record SourceProperties(
    @Validated @NotNull FSSourceSettings file
) { }
