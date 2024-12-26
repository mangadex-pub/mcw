package org.mangadex.mcw.output;

public interface Writer<T extends Output> {

    void flush(T target, String rendered) throws Exception;

}
