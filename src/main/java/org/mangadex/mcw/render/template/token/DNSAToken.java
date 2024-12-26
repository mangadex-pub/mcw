package org.mangadex.mcw.render.template.token;

public record DNSAToken(String value, int port) implements ServerToken { }
