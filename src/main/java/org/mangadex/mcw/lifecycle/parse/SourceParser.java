package org.mangadex.mcw.lifecycle.parse;

import java.time.Duration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.mangadex.mcw.lifecycle.dsn.Dsn;
import org.mangadex.mcw.source.Source;
import org.mangadex.mcw.source.SourceProperties;
import org.mangadex.mcw.source.file.FSSource;

@Component
public class SourceParser implements Converter<String, Source> {

    private final SourceProperties sourceProperties;

    public SourceParser(SourceProperties sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    @Override
    public Source convert(String source) {
        var dsn = Dsn.parse(source);

        return switch (dsn.protocol()) {
            case "file" -> FSSource.parse(dsn, Duration.ofSeconds(sourceProperties.file().checkPeriodSeconds()));
            default -> throw new UnsupportedOperationException("Unsupported source type: " + dsn.protocol());
        };
    }

}
