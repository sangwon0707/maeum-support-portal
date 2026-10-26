package kr.co.maeum.support.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceScheduler.class);
    private final MaintenanceService maintenance;

    public MaintenanceScheduler(MaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    /** 04시 초기화와 진행 중이던 작업·서버 중단으로 놓친 자료의 후속 정리를 함께 수행한다. */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void maintain() {
        try {
            int applications = maintenance.resetPublicAccount();
            int files = maintenance.collectOrphans();
            if (applications > 0 || files > 0)
                log.info("공개 계정 정리 applications={} files={}", applications, files);
        } catch (RuntimeException exception) {
            log.error("공개 계정 정리 실패 type={}", exception.getClass().getSimpleName());
        }
    }
}
