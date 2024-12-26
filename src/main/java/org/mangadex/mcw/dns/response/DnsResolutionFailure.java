package org.mangadex.mcw.dns.response;

public record DnsResolutionFailure(
    Throwable cause
) implements DnsResolution {

}
