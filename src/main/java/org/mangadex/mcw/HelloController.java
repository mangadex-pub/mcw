package org.mangadex.mcw;

import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.TEMPORARY_REDIRECT;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final String greeting;

    public HelloController(Environment environment) {
        this.greeting = "MCW version " + environment.getProperty("spring.application.version");
    }

    @GetMapping("/")
    public String hello() {
        return greeting;
    }

    @GetMapping("**")
    public ResponseEntity<?> redirectHome() {
        return ResponseEntity
            .status(TEMPORARY_REDIRECT)
            .header(LOCATION, "/")
            .build();
    }

}
