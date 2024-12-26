package org.mangadex.mcw.lifecycle.parse;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.mangadex.mcw.lifecycle.dsn.Dsn;
import org.mangadex.mcw.output.Output;
import org.mangadex.mcw.output.file.FSOutput;

@Component
public class OutputParser implements Converter<String, Output> {

    @Override
    public Output convert(String source) {
        var dsn = Dsn.parse(source);

        return switch (dsn.protocol()) {
            case "file" -> FSOutput.parse(dsn);
            default -> throw new UnsupportedOperationException("Unsupported output type: " + dsn.protocol());
        };
    }

}
