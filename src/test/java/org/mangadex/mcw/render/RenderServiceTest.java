package org.mangadex.mcw.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.io.IOException;

import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

import org.mangadex.mcw.dns.KnotSidecar;
import org.mangadex.mcw.render.template.InvalidTemplateException;

@SpringBootTest(properties = {
    "org.mangadex.mcw.render.ttl.min=10",
    "org.mangadex.mcw.render.ttl.max=86400"
})
@ImportTestcontainers(KnotSidecar.class)
class RenderServiceTest {

    @Autowired
    private RenderService rsvc;

    @Autowired
    private RenderProperties renderProperties;

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{ pools: null }",
        "{ pools: {} }"
    })
    void failsOnFalsyPools(String input) {
        assertThatThrownBy(() -> rsvc.render(input))
            .isInstanceOf(InvalidTemplateException.class)
            .hasMessageContaining("$.pools must be non-empty object");
    }

    @Test
    void preservesInputWithoutTokens() throws IOException, JSONException {
        @Language("JSON5")
        var config = """
            {
                "pools": {
                    "foo": {
                        "servers": [
                            "bar",
                            "baz"
                        ]
                    },
                    "empty-srvs": {
                        "servers": []
                    }
                }
            }
            """;

        assertRenders(config, new Render(config, renderProperties.ttl().max()));
    }

    @Test
    void replacesWithConsistentOrdering() throws IOException, JSONException {
        @Language("JSON5")
        var template = """
            {
                pools: {
                    "pool-a": {
                        servers: [
                            "foo:1234",
                            "bar:4567",
                            "dns://memcache-multi-a.mcw.mangadex",
                            "dns+1234://memcache-multi-a.mcw.mangadex",
                            "dnssrv://_memcache._tcp.memcache-srv.mcw.mangadex",
                        ]
                    },
                    "pool-b": {
                        servers: [
                            "dns://memcache-static-vip.mcw.mangadex",
                            "baz:0123"
                        ]
                    }
                }
            }
            """;

        @Language("JSON")
        var expected = """
            {
                "pools": {
                    "pool-a": {
                        "servers": [
                            "foo:1234",
                            "bar:4567",
                            "10.0.0.1:11211",
                            "10.0.0.2:11211",
                            "10.0.0.3:11211",
                            "10.0.0.1:1234",
                            "10.0.0.2:1234",
                            "10.0.0.3:1234",
                            "10.0.0.1:11211",
                            "10.0.0.2:11211",
                            "10.0.0.3:11211"
                        ]
                    },
                    "pool-b": {
                        "servers": [
                            "10.0.0.0:11211",
                            "baz:0123"
                        ]
                    }
                }
            }
            """;

        assertRenders(template, new Render(expected, 60L));
    }

    @Test
    void failsOnDnsLookupFailure() {
        var config = """
            {
                "pools": {
                    "foo": {
                        "servers": [
                            "dns://doesnotexist.mwc.mangadex"
                        ]
                    },
                }
            }
            """;

        assertThatThrownBy(() -> rsvc.render(config))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("doesnotexist.mwc.mangadex");
    }

    void assertRenders(@Language("JSON5") String input, Render expected) throws IOException, JSONException {
        var actual = rsvc.render(input);

        assertEquals(expected.rendered(), actual.rendered(), true);
        assertThat(actual.ttl()).isEqualTo(expected.ttl());
    }

}
