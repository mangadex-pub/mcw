package org.mangadex.mcw.source;

import java.util.Set;
import java.util.function.Consumer;

public interface Watcher<T extends Source> {

    void addWatch(T source, Consumer<String> onChanged);

    void removeWatch(T source);

    Set<T> getSources();

}
