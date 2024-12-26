package org.mangadex.mcw.lifecycle.dsn;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DsnTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "*",
        "not-a-uri",
        "opaque:uri",
    })
    void failsOnBadFormat(String input) {
        assertThatThrownBy(() -> Dsn.parse(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(input);
    }

    @Test
    void successNoParams() {
        var parsed = Dsn.parse("scheme://value");
        assertThat(parsed.protocol()).isEqualTo("scheme");
        assertThat(parsed.value()).isEqualTo("value");
        assertThat(parsed.parameters()).isEmpty();
    }

    @Test
    void successWithValuedParam() {
        var parsed = Dsn.parse("scheme://value?key=value");
        assertThat(parsed.protocol()).isEqualTo("scheme");
        assertThat(parsed.value()).isEqualTo("value");
        assertThat(parsed.parameters()).containsExactly(
            entry("key", List.of("value"))
        );
    }

    @Test
    void successWithNonValuedParam() {
        var parsed = Dsn.parse("scheme://value?key");
        assertThat(parsed.protocol()).isEqualTo("scheme");
        assertThat(parsed.value()).isEqualTo("value");

        // List#of(T...) enforces non-null elems
        var v = new ArrayList<String>();
        v.add(null);

        assertThat(parsed.parameters()).containsExactly(
            entry("key", v)
        );
    }

    @Test
    void successWithMultiParams() {
        var parsed = Dsn.parse("scheme://value?keyA=valueA1&keyA=valueA2&keyB=valueB1&keyB");
        assertThat(parsed.protocol()).isEqualTo("scheme");
        assertThat(parsed.value()).isEqualTo("value");

        var vB = new ArrayList<String>();
        vB.add("valueB1");
        vB.add(null);

        assertThat(parsed.parameters()).containsExactly(
            entry("keyA", List.of("valueA1", "valueA2")),
            entry("keyB", vB)
        );
    }

    @Test
    void successFilePath() {
        var parsed = Dsn.parse("file:///path/to/file");
        assertThat(parsed.protocol()).isEqualTo("file");
        assertThat(parsed.value()).isEqualTo("/path/to/file");
    }

}
