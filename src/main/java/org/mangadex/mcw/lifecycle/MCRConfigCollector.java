package org.mangadex.mcw.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import org.mangadex.mcw.lifecycle.parse.MCRConfigParser;

@Component
public class MCRConfigCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCRConfigCollector.class);

    private final MCRConfigParser MCRConfigParser;
    private final MCRConfigProperties MCRConfigProperties;

    public MCRConfigCollector(MCRConfigParser MCRConfigParser, MCRConfigProperties MCRConfigProperties) {
        this.MCRConfigParser = MCRConfigParser;
        this.MCRConfigProperties = MCRConfigProperties;
    }

    public List<MCRConfig> findAll(ApplicationArguments args) {
        List<MCRConfig> watches = new ArrayList<>();

        var argWatch = parseFromArguments(args);
        if (argWatch != null) {
            LOGGER.info("Found arguments-based watch: {}", argWatch);
            watches.add(argWatch);
        }

        var configWatches = parseFromConfig();
        if (configWatches != null && !configWatches.isEmpty()) {
            LOGGER.debug("Found configuration-based watches: {}", watches);
            watches.addAll(configWatches);
        }

        return watches;
    }

    private MCRConfig parseFromArguments(ApplicationArguments args) {
        LOGGER.trace("Parsing watch from {} arguments", args.getOptionNames().size());
        boolean hasSource = args.containsOption("source");
        boolean hasOutput = args.containsOption("output");
        if (!hasSource && !hasOutput) {
            LOGGER.trace("No source or output specified in arguments");
            return null;
        }

        if (hasSource != hasOutput) {
            throw new IllegalArgumentException("--source and --output arguments must both be set");
        }

        var sourceOpt = args.getOptionValues("source");
        if (sourceOpt.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one 'source' option");
        }

        var outputOpt = args.getOptionValues("output");
        if (outputOpt.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one 'output' option");
        }

        var watch = MCRConfigParser.parseWatch(sourceOpt.getFirst(), outputOpt.getFirst());
        LOGGER.trace("+ watch {}", watch);
        return watch;
    }

    private List<MCRConfig> parseFromConfig() {
        LOGGER.trace("Parsing configs from configuration: {}", MCRConfigProperties.configs());

        return MCRConfigProperties
            .configs()
            .stream()
            .map(wce -> MCRConfigParser.parseWatch(wce.source(), wce.output()))
            .peek(watch -> LOGGER.trace("+ watch {}", watch))
            .toList();
    }

}
