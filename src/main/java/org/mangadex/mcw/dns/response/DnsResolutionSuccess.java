package org.mangadex.mcw.dns.response;

import java.util.List;

import org.xbill.DNS.Record;

public record DnsResolutionSuccess(
    List<Record> records
) implements DnsResolution { }
