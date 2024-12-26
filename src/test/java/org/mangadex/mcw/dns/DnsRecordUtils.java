package org.mangadex.mcw.dns;

import java.net.InetAddress;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

public final class DnsRecordUtils {

    private DnsRecordUtils() {
    }

    public static org.xbill.DNS.ARecord ARecord(String name, int ttl, String value) {
        try {
            return new org.xbill.DNS.ARecord(Name.fromString(name, Name.root), DClass.IN, ttl, InetAddress.ofLiteral(value));
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static org.xbill.DNS.SRVRecord SRVRecord(String name, int ttl, int priority, int weight, int port, String value) {
        try {
            return new org.xbill.DNS.SRVRecord(Name.fromString(name, Name.root), DClass.IN, ttl, priority, weight, port, Name.fromString(value, Name.root));
        } catch (TextParseException e) {
            throw new RuntimeException(e);
        }
    }

}
