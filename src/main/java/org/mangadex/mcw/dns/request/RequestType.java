package org.mangadex.mcw.dns.request;

import org.xbill.DNS.Type;

public enum RequestType {

    A(Type.A),
    SRV(Type.SRV);

    public final int code;

    RequestType(int code) {
        this.code = code;
    }

}
