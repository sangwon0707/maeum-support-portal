package kr.co.maeum.support.maintenance;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class StorageGuardTest {
    @Test
    @DisplayName("OCR 파일을 사용하는 동안 수거가 기다리고 사용이 끝나면 실행한다")
    void collection_waits_for_in_progress_file_use() throws Exception {
        var guard = new StorageGuard();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var started = new CountDownLatch(1);
            java.util.concurrent.Future<?> collection;
            try (var use = guard.use()) {
                collection =
                        executor.submit(
                                () -> {
                                    started.countDown();
                                    guard.collect(() -> {});
                                });
                assertTrue(started.await(1, TimeUnit.SECONDS));
                assertThrows(
                        TimeoutException.class, () -> collection.get(100, TimeUnit.MILLISECONDS));
                var followUp =
                        executor.submit(
                                () -> {
                                    try (var callbackUse = guard.use()) {
                                        return true;
                                    }
                                });
                assertTrue(followUp.get(1, TimeUnit.SECONDS));
            }
            collection.get(1, TimeUnit.SECONDS);
        }
    }
}
