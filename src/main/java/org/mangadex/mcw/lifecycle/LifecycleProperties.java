package org.mangadex.mcw.lifecycle;

import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "org.mangadex.mcw.lifecycle")
public record LifecycleProperties(
    @Validated @Positive int retryDelaySeconds
) { }
