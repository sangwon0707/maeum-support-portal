package kr.co.maeum.support.maintenance;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.config.DatabaseConfig;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 내용 주소 저장소. 파일 공유 중 롤백 보상 삭제는 하지 않고 참조 확인 후 수거한다. */
@Component
public class ContentFileStore {
    private final Path root;
    private final StorageGuard guard;

    public ContentFileStore(StorageGuard guard) throws IOException {
        this.guard = guard;
        root =
                Path.of(DatabaseConfig.env("FILE_ROOT", ".local/files"))
                        .toAbsolutePath()
                        .normalize();
        Files.createDirectories(root.resolve("intake"));
    }

    public Path root() {
        return root;
    }

    public String save(byte[] bytes, boolean intake, String extension) {
        guard.retainUntilCommit();
        try (var ignored = guard.use()) {
            return write(bytes, intake, extension);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("파일을 저장하지 못했습니다.", exception);
        }
    }

    private synchronized String write(byte[] bytes, boolean intake, String extension)
            throws IOException {
        String key;
        try {
            key =
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
                            + extension;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
        Path directory = intake ? root.resolve("intake") : root;
        Path target = directory.resolve(key);
        if (Files.isRegularFile(target)) return key;
        long used;
        try (var files = Files.walk(root)) {
            used =
                    files.filter(Files::isRegularFile)
                            .mapToLong(
                                    path -> {
                                        try {
                                            return Files.size(path);
                                        } catch (IOException exception) {
                                            throw new java.io.UncheckedIOException(exception);
                                        }
                                    })
                            .sum();
        }
        long maximum = Long.parseLong(DatabaseConfig.env("FILE_MAX_BYTES", "536870912"));
        long reserve = Long.parseLong(DatabaseConfig.env("FILE_MIN_FREE_BYTES", "1073741824"));
        if (bytes.length > maximum - used
                || Files.getFileStore(root).getUsableSpace() - bytes.length < reserve) {
            throw new BusinessException(507, "서류 저장 공간이 부족합니다. 자료 정리 후 다시 이용해 주세요.");
        }
        Path temporary = Files.createTempFile(directory, ".upload-", ".tmp");
        try {
            Files.write(temporary, bytes);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return key;
    }
}
