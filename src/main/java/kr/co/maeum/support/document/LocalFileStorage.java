package kr.co.maeum.support.document;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalFileStorage implements FileStorage {
    private final Path root;

    private final kr.co.maeum.support.maintenance.ContentFileStore files;

    public LocalFileStorage(kr.co.maeum.support.maintenance.ContentFileStore files) {
        this.files = files;
        root = files.root();
    }

    @Override
    public String save(byte[] content) {
        return files.save(content, false, ".png");
    }

    @Override
    public Path resolve(String key) {
        if (key == null || !key.matches("(?:[a-f0-9-]{36}|[a-f0-9]{64})\\.png")) {
            throw new IllegalArgumentException("잘못된 저장 키입니다.");
        }
        return root.resolve(key);
    }

    @Override
    public void delete(String key) {
        // 내용 주소 파일은 다른 신청이 참조할 수 있어 정기 참조 검사에서만 삭제한다.
        if (key != null && key.matches("[a-f0-9]{64}\\.(pdf|png)")) return;
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("파일 정리가 필요합니다.", exception);
        }
    }
}
