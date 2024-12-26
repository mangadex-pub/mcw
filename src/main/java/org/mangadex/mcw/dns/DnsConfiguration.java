package org.mangadex.mcw.dns;

import static java.lang.Integer.parseInt;

import java.net.UnknownHostException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SimpleResolver;

import org.mangadex.mcw.dns.DnsProperties.DnsDiscovery;
import org.mangadex.mcw.dns.DnsProperties.DnsOptions;

@Configuration(proxyBeanMethods = false)
@ConfigurationPropertiesScan
public class DnsConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsConfiguration.class);

    @Bean
    public Resolver resolver(DnsProperties dnsProperties) {
        ExtendedResolver resolver;

        if (dnsProperties.discovery() == DnsDiscovery.STATIC) {
            Objects.requireNonNull(dnsProperties.nameservers(), "Static discovery requires non-null nameservers");
            LOGGER.info("Using static DNS servers: {}", dnsProperties.nameservers());
            var srs = dnsProperties.nameservers().stream().map(DnsConfiguration::hostportResolver).toList();
            resolver = new ExtendedResolver(srs.toArray(new Resolver[0]));
        } else {
            var servers = ResolverConfig.getCurrentConfig().servers();
            LOGGER.info("Using discovered DNS servers: {}", servers);
            resolver = new ExtendedResolver();
            servers.forEach(addr -> resolver.addResolver(new SimpleResolver(addr)));
            return resolver;
        }

        if (!dnsProperties.options().isEmpty()) {
            LOGGER.info("Using DNS options: {}", dnsProperties.options());
            if (dnsProperties.options().contains(DnsOptions.FORCE_TCP)) {
                resolver.setTCP(true);
            }
        }

        return resolver;
    }

    private static SimpleResolver hostportResolver(String hostport) {
        try {
            var parts = hostport.split(":");
            var sr = new SimpleResolver(parts[0]);
            sr.setPort(parseInt(parts[1]));
            return sr;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }

}
