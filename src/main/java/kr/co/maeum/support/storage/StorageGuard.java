package kr.co.maeum.support.storage;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/** 파일 저장 트랜잭션·OCR 작업이 끝나기 전에는 미참조 파일을 수거하지 않는다. */
@Component
public class StorageGuard {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public AutoCloseable use() {
        acquireUse();
        return () -> lock.readLock().unlock();
    }

    public void retainUntilCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        acquireUse();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        lock.readLock().unlock();
                    }
                });
    }

    private void acquireUse() {
        // OCR 이벤트는 다른 스레드에서 저장한다. 대기 중인 수거가 기존 작업의 후속 저장을 막으면 교착된다.
        if (!lock.readLock().tryLock()) {
            lock.readLock().lock();
        }
    }

    public void collect(Runnable action) {
        lock.writeLock().lock();
        try {
            action.run();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
