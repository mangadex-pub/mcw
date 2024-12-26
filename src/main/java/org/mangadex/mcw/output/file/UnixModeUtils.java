package org.mangadex.mcw.output.file;

import static java.util.stream.Collectors.joining;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class UnixModeUtils {

    private static final int PERM_R = 0b100;
    private static final int PERM_W = 0b010;
    private static final int PERM_X = 0b001;

    private static final Map<Integer, Set<PosixFilePermission>> CACHE = new HashMap<>();

    private UnixModeUtils() {
    }

    public static Set<PosixFilePermission> toPermissions(int permissionsNumber) {
        if (CACHE.containsKey(permissionsNumber)) {
            return CACHE.get(permissionsNumber);
        }

        var mask = 0b111;

        var owner = permissionsNumber >> 6 & mask;
        var group = permissionsNumber >> 3 & mask;
        var world = permissionsNumber & mask;

        var permissionsString = Stream.of(owner, group, world).map(principal -> {
            var perm = "";
            perm += ((principal & PERM_R) == PERM_R) ? 'r' : '-';
            perm += ((principal & PERM_W) == PERM_W) ? 'w' : '-';
            perm += ((principal & PERM_X) == PERM_X) ? 'x' : '-';
            return perm;
        }).collect(joining());

        var result = PosixFilePermissions.fromString(permissionsString);
        CACHE.put(permissionsNumber, result);
        return result;
    }

}
