package org.mangadex.mcw.render.template;

import org.mangadex.mcw.render.template.token.ServerToken;

public record ResolvedServer(String value, ServerToken source, long ttl) {
}
