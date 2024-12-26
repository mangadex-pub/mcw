package org.mangadex.mcw.dns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mangadex.mcw.dns.DnsRecordUtils.ARecord;
import static org.mangadex.mcw.dns.DnsRecordUtils.SRVRecord;

import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.xbill.DNS.lookup.NoSuchDomainException;

import org.mangadex.mcw.dns.request.DnsRequest;
import org.mangadex.mcw.dns.request.RequestType;
import org.mangadex.mcw.dns.response.DnsResolution;
import org.mangadex.mcw.dns.response.DnsResolutionFailure;
import org.mangadex.mcw.dns.response.DnsResolutionSuccess;

@SpringBootTest(classes = {
    DnsResolver.class,
    DnsConfiguration.class,
})
@ImportTestcontainers(KnotSidecar.class)
class DnsResolverTest {

    @Autowired
    private DnsResolver resolver;

    @Nested
    public class A {

        @Test
        @DisplayName("Handles a single A record")
        void successSingle() {
            var response = resolver.resolve(new DnsRequest(RequestType.A, "memcache-static-vip.mcw.mangadex"));
            assertSuccessfulAnd(
                response,
                success -> assertThat(success.records()).containsExactly(
                    ARecord("memcache-static-vip.mcw.mangadex", 60, "10.0.0.0")
                )
            );
        }

        @Test
        @DisplayName("Handles multiple A records on the same name")
        void successMultiple() {
            var response = resolver.resolve(new DnsRequest(RequestType.A, "memcache-multi-a.mcw.mangadex"));
            assertSuccessfulAnd(
                response,
                success -> assertThat(success.records()).containsExactly(
                    ARecord("memcache-multi-a.mcw.mangadex", 60, "10.0.0.1"),
                    ARecord("memcache-multi-a.mcw.mangadex", 60, "10.0.0.2"),
                    ARecord("memcache-multi-a.mcw.mangadex", 60, "10.0.0.3")
                )
            );
        }

        @Test
        @DisplayName("Handles CNAMEs by returning only the final resolution")
        void successCnameSingle() {
            var response = resolver.resolve(new DnsRequest(RequestType.A, "mc-vip.mcw.mangadex"));
            assertSuccessfulAnd(
                response,
                success -> assertThat(success.records()).containsExactly(
                    ARecord("memcache-static-vip.mcw.mangadex", 60, "10.0.0.0")
                )
            );
        }

        @Test
        @DisplayName("Handles exceptions by conserving the cause")
        void handlesExceptions() {
            var response = resolver.resolve(new DnsRequest(RequestType.A, "does-not-exist.mcw.mangadex"));
            assertFailedAnd(
                response,
                failure -> assertThat(failure.cause()).isInstanceOf(NoSuchDomainException.class)
            );
        }

    }

    @Nested
    public class SRV {

        @Test
        @DisplayName("Handles direct SRV resolution")
        void successDirect() {
            var response = resolver.resolve(new DnsRequest(RequestType.SRV, "_memcache._tcp.memcache-srv.mcw.mangadex"));
            assertSuccessfulAnd(
                response,
                success -> assertThat(success.records()).containsExactly(
                    SRVRecord("_memcache._tcp.memcache-srv.mcw.mangadex", 60, 10, 100, 11211, "memcache-static-1.mcw.mangadex"),
                    SRVRecord("_memcache._tcp.memcache-srv.mcw.mangadex", 60, 20, 100, 11211, "memcache-static-2.mcw.mangadex"),
                    SRVRecord("_memcache._tcp.memcache-srv.mcw.mangadex", 60, 30, 100, 11211, "memcache-static-3.mcw.mangadex")
                )
            );
        }

        @Test
        @DisplayName("Handles SRV queries where query hits a CNAME first")
        void cnameToSrvQuery() {
            var response = resolver.resolve(new DnsRequest(RequestType.SRV, "mc-srv-to-query.mcw.mangadex"));
            assertSuccessfulAnd(
                response,
                success -> assertThat(success.records()).containsExactly(
                    SRVRecord("_memcache._tcp.memcache-srv.mcw.mangadex", 60, 10, 100, 11211, "memcache-static-1.mcw.mangadex"),
                    SRVRecord("_memcache._tcp.memcache-srv.mcw.mangadex", 60, 20, 100, 11211, "memcache-static-2.mcw.mangadex"),
                    SRVRecord("_memcache._tcp.memcache-srv.mcw.mangadex", 60, 30, 100, 11211, "memcache-static-3.mcw.mangadex")
                )
            );
        }

        @Test
        @DisplayName("Handles SRV queries where rdata is a CNAME (disallowed by RFC2782)")
        void srvToCname() {
            var response = resolver.resolve(new DnsRequest(RequestType.SRV, "_memcache._tcp.mc-vip.mcw.mangadex"));
            assertSuccessfulAnd(
                response,
                success -> assertThat(success.records()).containsExactly(
                    SRVRecord("_memcache._tcp.mc-vip.mcw.mangadex", 60, 10, 100, 11211, "mc-vip.mcw.mangadex")
                )
            );
        }

        @Test
        @DisplayName("Handles exceptions by conserving the cause")
        void handlesExceptions() {
            var response = resolver.resolve(new DnsRequest(RequestType.SRV, "_memcache._tcp.does-not-exist.mcw.mangadex"));
            assertFailedAnd(
                response,
                failure -> assertThat(failure.cause()).isInstanceOf(NoSuchDomainException.class)
            );
        }

    }

    private void assertSuccessfulAnd(DnsResolution response, Consumer<DnsResolutionSuccess> assertions) {
        assertThat(response).isInstanceOfSatisfying(DnsResolutionSuccess.class, assertions);
    }

    private void assertFailedAnd(DnsResolution response, Consumer<DnsResolutionFailure> assertions) {
        assertThat(response).isInstanceOfSatisfying(DnsResolutionFailure.class, assertions);
    }


}
