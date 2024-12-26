package org.mangadex.mcw.dns;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

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

    private static final File KNOTDNS_COMPOSE_FILE = Paths.get("docker-compose.yml").toFile();
    private static final int KNOTDNS_PORT_DNS = 15353;

    @Container
    static ComposeContainer knotdns = new ComposeContainer(KNOTDNS_COMPOSE_FILE)
        .withLocalCompose(true)
        .withTailChildContainers(true)
        .waitingFor("knotdns", new HostPortWaitStrategy());

    @DynamicPropertySource
    static void testNameservers(DynamicPropertyRegistry registry) {
        knotdns.start();
        knotdns.waitingFor("knotdns", new HostPortWaitStrategy());
        var host = knotdns.getServiceHost("knotdns", KNOTDNS_PORT_DNS);
        registry.add("org.mangadex.mcw.dns.discovery", () -> DnsDiscovery.STATIC);
        registry.add("org.mangadex.mcw.dns.nameservers", () -> List.of("%s:%d".formatted(host, KNOTDNS_PORT_DNS)));
        registry.add("org.mangadex.mcw.dns.options", () -> List.of(DnsOptions.FORCE_TCP));
    }

}
