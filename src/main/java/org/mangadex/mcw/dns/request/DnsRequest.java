package org.mangadex.mcw.dns.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

@Validated
public record DnsRequest(
    @NotNull RequestType type,
    @NotBlank String name
) {

    public Name qname() {
        try {
            return Name.fromString(name(), Name.root);
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }

}
