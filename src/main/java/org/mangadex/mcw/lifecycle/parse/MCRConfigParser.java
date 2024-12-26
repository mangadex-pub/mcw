package org.mangadex.mcw.lifecycle.parse;

import org.springframework.stereotype.Component;

import org.mangadex.mcw.lifecycle.MCRConfig;

@Component
public class MCRConfigParser {

    private final SourceParser sourceParser;
    private final OutputParser outputParser;

    public MCRConfigParser(SourceParser sourceParser, OutputParser outputParser) {
        this.sourceParser = sourceParser;
        this.outputParser = outputParser;
    }

    public MCRConfig parseWatch(String sourceSpec, String outputSpec) {
        var source = sourceParser.convert(sourceSpec);
        var output = outputParser.convert(outputSpec);

        return new MCRConfig(source, output);
    }

}
