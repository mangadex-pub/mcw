package org.mangadex.mcw.dns;

import java.util.List;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "org.mangadex.mcw.dns")
public record DnsProperties(

    @NotNull
    DnsProperties.DnsDiscovery discovery,

    @Nullable
    @UniqueElements
    List<@NotNull @Pattern(regexp = "^.+:\\d+") String> nameservers,

    @UniqueElements
    List<@NotNull DnsOptions> options

) {

    public enum DnsDiscovery {
        AUTO,
        STATIC,
    }

    public enum DnsOptions {
        FORCE_TCP
    }

}
