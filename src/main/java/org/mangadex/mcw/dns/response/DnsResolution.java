package org.mangadex.mcw.dns.response;

public sealed interface DnsResolution permits DnsResolutionFailure, DnsResolutionSuccess {
}
