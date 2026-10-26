package kr.co.maeum.support.storage;

import java.nio.file.Path;

public interface FileStorage {
    String save(byte[] content);

    Path resolve(String key);

    void delete(String key);
}
