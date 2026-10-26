package kr.co.maeum.support.intake;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class IntakeStorage {
    private final Path root;

    private final kr.co.maeum.support.maintenance.ContentFileStore files;

    public IntakeStorage(kr.co.maeum.support.maintenance.ContentFileStore files) {
        this.files = files;
        root = files.root().resolve("intake");
    }

    public String save(byte[] bytes, boolean pdf) {
        return files.save(bytes, true, pdf ? ".pdf" : ".png");
    }

    public Path resolve(String key) {
        if (key == null || !key.matches("(?:[a-f0-9-]{36}|[a-f0-9]{64})\\.(pdf|png)"))
            throw new IllegalArgumentException("잘못된 문서 키입니다.");
        return root.resolve(key);
    }

    public void delete(String key) {
        // 내용 주소 파일은 다른 신청이 참조할 수 있어 정기 참조 검사에서만 삭제한다.
        if (key != null && key.matches("[a-f0-9]{64}\\.(pdf|png)")) return;
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("문서 파일 정리가 필요합니다.", exception);
        }
    }
}
