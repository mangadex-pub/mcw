package org.mangadex.mcw.render.template.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.ListAssert;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import org.mangadex.mcw.render.RenderConfiguration;
import org.mangadex.mcw.render.RenderProperties;

@SpringBootTest(classes = {
    JacksonAutoConfiguration.class,
    PoolTokenizer.class,
    RenderConfiguration.class,
})
class PoolTokenizerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PoolTokenizer tokenizer;

    @Autowired
    private RenderProperties settings;

    @Nested
    @DisplayName("Successfully tokenizes")
    public class Valid {

        @Test
        @DisplayName("an empty pool")
        void emptyPool() throws JsonProcessingException {
            assertTokenized("{ servers: [] }").isEmpty();
        }

        @Test
        @DisplayName("a single DNS A server token with default port")
        void singleANoPort() throws JsonProcessingException {
            assertTokenizes(
                "{ servers: [ 'dns://default.port.example' ] }",
                new DNSAToken("default.port.example", settings.defaultPort())
            );
        }

        @Test
        @DisplayName("a single DNS A server token with custom port")
        void singleAWithPort() throws JsonProcessingException {
            assertTokenizes(
                "{ servers: [ 'dns+1234://custom.port.example' ] }",
                new DNSAToken("custom.port.example", 1234)
            );
        }

        @Test
        @DisplayName("a single DNS SRV server token")
        void singleDnsSrv() throws JsonProcessingException {
            assertTokenizes(
                "{ servers: ['dnssrv://_memcache._tcp.srv.example' ] }",
                new DNSSRVToken("_memcache._tcp.srv.example")
            );
        }

        @Test
        @DisplayName("a single fixed server token")
        void singleFixed() throws JsonProcessingException {
            assertTokenizes(
                "{ servers: ['fixed:4567' ] }",
                new FixedToken("fixed:4567")
            );
        }

        @Test
        void multiMixed() throws JsonProcessingException {
            assertTokenizes(
                """
                    {
                        servers: [
                            'fixed:4567',
                            'dns://default.port.example',
                            'dns+1234://custom.port.example',
                            'dnssrv://_memcache._tcp.srv.example',
                            'fixed:7890',
                        ]
                    }
                    """,
                new FixedToken("fixed:4567"),
                new DNSAToken("default.port.example", settings.defaultPort()),
                new DNSAToken("custom.port.example", 1234),
                new DNSSRVToken("_memcache._tcp.srv.example"),
                new FixedToken("fixed:7890")
            );
        }

        private void assertTokenizes(@Language("JSON5") String input, ServerToken... expected) throws JsonProcessingException {
            assertTokenized(input).containsExactly(expected);
        }

        private ListAssert<ServerToken> assertTokenized(@Language("JSON5") String input) throws JsonProcessingException {
            return assertThat(tokenizer.tokenize("test-pool", objectMapper.readTree(input)));
        }

    }

    @Nested
    @DisplayName("Fails tokenization")
    public class Invalid {

        @Test
        void failsOnNullPool() {
            assertTokenizationFailure(
                "null",
                "is null"
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
            // not exhaustive, sue me
            "[]",
            "\"string\"",
            "123",
        })
        void failsOnNonArrayPool(@Language("JSON5") String input) throws JsonProcessingException {
            var jsonType = objectMapper.readTree(input).getNodeType();
            assertTokenizationFailure(
                input,
                "is not a json object",
                jsonType.name()
            );
        }

        @Test
        void failsOnAbsentServerProperty() {
            assertTokenizationFailure(
                "{}",
                "has no 'servers' field"
            );
        }

        @Test
        void failsOnNullServersProperty() {
            assertTokenizationFailure(
                "{ servers: null }",
                "servers is null"
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "{}",
            "\"string\"",
            "1234",
        })
        void failsOnNonArrayServersProperty(String servers) throws JsonProcessingException {
            var jsonType = objectMapper.readTree(servers).getNodeType();
            assertTokenizationFailure(
                "{ servers: %s }".formatted(servers),
                "servers is not a json array",
                jsonType.name()
            );
        }

        @Test
        void failsOnOnNullServer() {
            assertTokenizationFailure(
                "{ servers: [ '1.2.3.4:1234', null ] }",
                "is null"
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "[]",
            "{}",
            "1234",
        })
        void failsOnNonStringServer(String server) throws JsonProcessingException {
            var jsonType = objectMapper.readTree(server).getNodeType();
            assertTokenizationFailure(
                "{ servers: [ '1.2.3.4:1234', %s ] }".formatted(server),
                "is not a string",
                jsonType.name()
            );
        }

        @Test
        void failsOnBlankStringServer() {
            assertTokenizationFailure(
                "{ servers: [ '1.2.3.4:1234', ' ' ] }",
                "is a blank string"
            );
        }

        @Test
        void failsOnInvalidDNSAServerToken() {
            assertTokenizationFailure(
                "{ servers: [ '1.2.3.4:1234', 'dns+abc://a.server.example' ] }",
                "is not a valid server token",
                "dns+abc://a.server.example"
            );
        }

        private void assertTokenizationFailure(@Language("JSON5") String input, String... messageContains) {
            assertThatThrownBy(() -> tokenizer.tokenize("test-pool", objectMapper.readTree(input)))
                .isInstanceOf(PoolTokenizationException.class)
                .hasMessageContaining("test-pool")
                .hasMessageContainingAll(messageContains);
        }

    }

}
