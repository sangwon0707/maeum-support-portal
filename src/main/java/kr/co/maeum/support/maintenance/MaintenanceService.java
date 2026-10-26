package kr.co.maeum.support.maintenance;

import kr.co.maeum.support.config.DatabaseConfig;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MaintenanceService {
    private final MaintenanceMapper mapper;
    private final TransactionTemplate transaction;
    private final ContentFileStore files;
    private final StorageGuard guard;
    private final Clock clock;
    private static final Pattern KEY =
            Pattern.compile("(?:[a-f0-9-]{36}|[a-f0-9]{64})\\.(?:pdf|png)");

    public MaintenanceService(
            MaintenanceMapper mapper,
            PlatformTransactionManager manager,
            ContentFileStore files,
            StorageGuard guard,
            Clock clock) {
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(manager);
        this.files = files;
        this.guard = guard;
        this.clock = clock;
    }

    public int resetPublicAccount() {
        var koreanNow = clock.instant().atZone(ZoneId.of("Asia/Seoul"));
        var cutoff = koreanNow.toLocalDate().atTime(4, 0).atZone(ZoneId.of("Asia/Seoul"));
        if (koreanNow.isBefore(cutoff)) cutoff = cutoff.minusDays(1);
        return resetBefore(
                DatabaseConfig.env("PUBLIC_DEMO_LOGIN", "applicant"),
                LocalDateTime.ofInstant(cutoff.toInstant(), ZoneOffset.UTC));
    }

    int resetBefore(String login, LocalDateTime cutoff) {
        int removed = 0;
        for (long id : mapper.candidates(login, cutoff)) {
            boolean deleted =
                    Boolean.TRUE.equals(
                            transaction.execute(
                                    status -> {
                                        // 업로드·반영과 같은 신청 행을 잠근 뒤 진행 중 작업은 완료될 때까지 보존한다.
                                        if (mapper.lockCandidate(id, login, cutoff) == null
                                                || mapper.activeJobs(id) > 0) return false;
                                        mapper.deleteConfirmations(id);
                                        mapper.deleteOcrResults(id);
                                        mapper.deleteOcrJobs(id);
                                        mapper.deleteRevisionDocuments(id);
                                        mapper.deleteSupplementResponses(id);
                                        mapper.deleteSupplementItems(id);
                                        mapper.deleteSupplements(id);
                                        mapper.deleteCommands(id);
                                        mapper.deleteRevisions(id);
                                        mapper.deleteDocuments(id);
                                        mapper.deleteIntakeSources(id);
                                        mapper.deleteIntakes(id);
                                        mapper.deleteReviews(id);
                                        mapper.deleteAssignments(id);
                                        mapper.deleteHistory(id);
                                        mapper.deleteAudit(id);
                                        mapper.deleteApplication(id);
                                        return true;
                                    }));
            if (deleted) removed++;
        }
        mapper.expireCache(login, cutoff);
        return removed;
    }

    public int collectOrphans() {
        int[] removed = {0};
        guard.collect(
                () -> {
                    Set<String> live = new HashSet<>();
                    live.addAll(mapper.documentKeys());
                    mapper.intakeKeys().forEach(key -> live.add("intake/" + key));
                    var payloads = new java.util.ArrayList<>(mapper.resultPayloads());
                    payloads.addAll(mapper.cachePayloads());
                    for (String payload : payloads) {
                        var matches = KEY.matcher(payload);
                        while (matches.find()) live.add("intake/" + matches.group());
                    }
                    // 장애 종료로 DB 참조가 생기지 않은 파일도 수거하되 신규 파일은 한 시간 보호한다.
                    var oldest = clock.instant().minus(Duration.ofHours(1));
                    try (var paths = Files.walk(files.root())) {
                        for (var path : paths.filter(Files::isRegularFile).toList()) {
                            String relative =
                                    files.root().relativize(path).toString().replace('\\', '/');
                            if (!live.contains(relative)
                                    && Files.getLastModifiedTime(path).toInstant().isBefore(oldest)
                                    && (KEY.matcher(path.getFileName().toString()).matches()
                                            || path.getFileName()
                                                    .toString()
                                                    .startsWith(".upload-"))) {
                                Files.deleteIfExists(path);
                                removed[0]++;
                            }
                        }
                    } catch (IOException exception) {
                        throw new IllegalStateException("미참조 파일 정리에 실패했습니다.", exception);
                    }
                });
        return removed[0];
    }
}
