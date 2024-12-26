package org.mangadex.mcw;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import org.mangadex.mcw.lifecycle.MCRConfigCollector;
import org.mangadex.mcw.lifecycle.MCRWatchLifecycler;

@Component
public class Bootstrap implements ApplicationRunner {

    private final MCRConfigCollector configCollector;
    private final MCRWatchLifecycler watchLifecycler;

    public Bootstrap(MCRConfigCollector configCollector, MCRWatchLifecycler watchLifecycler) {
        this.configCollector = configCollector;
        this.watchLifecycler = watchLifecycler;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        configCollector.findAll(args).forEach(watchLifecycler::register);
    }

}
