package org.mangadex.mcw.output.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UnixModeUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "0000 ---------",
        "0600 rw-------",
        "0644 rw-r--r--",
        "0700 rwx------",
        "0755 rwxr-xr-x",
        "0777 rwxrwxrwx",
    })
    void conversCorrectly(String input) {
        var args = input.split(" ", 2);

        var mode = Integer.parseInt(args[0], 8);
        var expected = PosixFilePermissions.fromString(args[1]);
        var actual = UnixModeUtils.toPermissions(mode);

        assertThat(actual).isEqualTo(expected);
    }

}
