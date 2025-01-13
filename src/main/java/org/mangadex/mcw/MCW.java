package org.mangadex.mcw;

import static java.util.Arrays.asList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import org.mangadex.mcw.lifecycle.MCRLifecycle.BuildInfo;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MCW {

    public static void main(String[] args) {
        var buildInfo = BuildInfo.fromApplicationYaml();
        if (asList(args).contains("--version")) {
            System.out.printf("MCW version %s (%s)\n", buildInfo.version(), buildInfo.timestamp());
            return;
        }

        SpringApplication.run(MCW.class, args);
    }

}
