package org.mangadex.mcw.render.template.token;

public sealed interface ServerToken permits DNSAToken, DNSSRVToken, FixedToken {

    String value();

}
