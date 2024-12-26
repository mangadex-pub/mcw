package org.mangadex.mcw.source.file;

import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "org.mangadex.mcw.source.file")
public record FSSourceSettings(
    @Positive int checkPeriodSeconds
) { }
