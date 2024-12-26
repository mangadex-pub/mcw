package org.mangadex.mcw.lifecycle;

import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import org.mangadex.mcw.output.Output;
import org.mangadex.mcw.source.Source;

@Validated
public record MCRConfig(
    @NotNull @Validated Source source,
    @NotNull @Validated Output output
) { }
