package org.mangadex.mcw.lifecycle;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "org.mangadex.mcw")
public record MCRConfigProperties(

    @Validated
    @NotNull
    @UniqueElements
    List<@Valid @NotNull MCRConfigProperty> configs

) {

    @Validated
    public record MCRConfigProperty(
        @NotNull @Pattern(regexp = "^[a-z]+://.+") String source,
        @NotNull @Pattern(regexp = "^[a-z]+://.+") String output
    ) { }

}
