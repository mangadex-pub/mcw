package org.mangadex.mcw.render.template.token;

import org.mangadex.mcw.render.template.InvalidTemplateException;

public class PoolTokenizationException extends InvalidTemplateException {

    public PoolTokenizationException(String poolName, String message) {
        super("pool[" + poolName + "]: " + message);
    }

}
