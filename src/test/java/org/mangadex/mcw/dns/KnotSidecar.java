package org.mangadex.mcw.dns;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Container;

import org.mangadex.mcw.dns.DnsProperties.DnsDiscovery;
import org.mangadex.mcw.dns.DnsProperties.DnsOptions;

@TestConfiguration(proxyBeanMethods = false)
public class KnotSidecar {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnotSidecar.class);

    private static final File KNOTDNS_COMPOSE_FILE = Paths.get("docker-compose.yml").toFile();
    private static final int KNOTDNS_PORT_DNS = 5353;

    @Container
    static ComposeContainer knotdns = new ComposeContainer(KNOTDNS_COMPOSE_FILE)
        .withLocalCompose(true)
        .withExposedService("knotdns", KNOTDNS_PORT_DNS)
        .withTailChildContainers(true)
        .waitingFor("knotdns", new HostPortWaitStrategy().forPorts(KNOTDNS_PORT_DNS));

    @DynamicPropertySource
    static void testNameservers(DynamicPropertyRegistry registry) {
        knotdns.start();
        var host = knotdns.getServiceHost("knotdns", KNOTDNS_PORT_DNS);
        var port = knotdns.getServicePort("knotdns", KNOTDNS_PORT_DNS);
        LOGGER.info("Test KnotDNS server: '{}:{}'", host, port);

        registry.add("org.mangadex.mcw.dns.discovery", () -> DnsDiscovery.STATIC);
        registry.add("org.mangadex.mcw.dns.options", () -> List.of(DnsOptions.FORCE_TCP));
        registry.add("org.mangadex.mcw.dns.nameservers", () -> List.of(host + ":" + port));
    }

}
