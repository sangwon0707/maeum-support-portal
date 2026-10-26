package kr.co.maeum.support.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.co.maeum.support.common.JsonCodec;
import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.service.DemoService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.DataSource;

/** 부하 자료는 분리된 테스트 DB에만 만든다. HTTP 성능이나 운영 성능을 뜻하지 않는다. */
@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
class SqlPerformanceTest {
    @Autowired DataSource dataSource;
    @Autowired ApplicationMapper applications;
    @Autowired ReportingMapper reports;
    @Autowired DemoService demos;
    @Autowired IdentityMapper identities;
    @Autowired JsonCodec json;

    @Test
    @DisplayName("가상 신청 만 건의 검색·집계를 10개 동시 작업으로 측정한다")
    void list_and_aggregation_on_ten_thousand_rows() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertTrue(jdbc.queryForObject("SELECT DATABASE()", String.class).endsWith("_test"));
        jdbc.update("INSERT INTO workspace(kind) VALUES('NORMAL')");
        long workspaceId = jdbc.queryForObject("SELECT MAX(id) FROM workspace", Long.class);
        String suffix = UUID.randomUUID().toString();
        demos.createUser(workspaceId, "bench-staff-" + suffix, "집계 검증 담당자", "STAFF", suffix);
        long staffId = identities.findRole(workspaceId, "STAFF").id();
        jdbc.update(
                """
                INSERT INTO app_user(workspace_id,login_id,password_hash,display_name,status)
                SELECT ?,CONCAT('bench-',?,'-',seq),'unused','가상 사업자','LOCKED'
                  FROM seq_1_to_10000
                """,
                workspaceId,
                workspaceId);
        jdbc.update(
                """
                INSERT INTO application(workspace_id,program_id,owner_id,receipt_no,status,business_name,
                                        submitted_at,updated_at)
                SELECT ?,1,u.id,CONCAT('BENCH-',?, '-',u.id),
                       CASE MOD(u.id,3) WHEN 0 THEN 'SUBMITTED' WHEN 1 THEN 'UNDER_REVIEW' ELSE 'APPROVED' END,
                       CONCAT('가상상점-',u.id),UTC_TIMESTAMP(6),TIMESTAMPADD(SECOND,-u.id,UTC_TIMESTAMP(6))
                  FROM app_user u WHERE workspace_id=? AND status='LOCKED'
                """,
                workspaceId,
                workspaceId,
                workspaceId);
        jdbc.update(
                """
                INSERT INTO staff_assignment(application_id,staff_id,assigned_by,reason)
                SELECT id,?,?,'부하 검증용 가상 배정' FROM application WHERE workspace_id=? AND MOD(id,2)=0
                """,
                staffId,
                staffId,
                workspaceId);
        Map<String, Object> scope = new HashMap<>();
        scope.put("workspaceId", workspaceId);
        scope.put("staffId", staffId);
        scope.put("keyword", "");
        scope.put("status", "SUBMITTED");
        scope.put("limit", 20);
        scope.put("offset", 0);
        assertEquals(
                10000,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM application WHERE workspace_id=?",
                        Integer.class,
                        workspaceId));
        long total =
                reports.statusCounts(scope).stream()
                        .mapToLong(row -> ((Number) row.get("total")).longValue())
                        .sum();
        assertEquals(5000, total);
        String select =
                "SELECT id FROM application %s WHERE workspace_id=? AND status='SUBMITTED' ORDER BY"
                        + " updated_at DESC,id DESC LIMIT 20";
        var before =
                jdbc.queryForList(
                        "EXPLAIN "
                                + select.formatted(
                                        "IGNORE"
                                            + " INDEX(ix_application_sort,ix_application_status_sort)"),
                        workspaceId);
        var after = jdbc.queryForList("EXPLAIN " + select.formatted(""), workspaceId);
        assertEquals(
                jdbc.queryForList(
                        select.formatted(
                                "IGNORE INDEX(ix_application_sort,ix_application_status_sort)"),
                        workspaceId),
                jdbc.queryForList(select.formatted(""), workspaceId));
        for (int i = 0; i < 10; i++) {
            applications.list(scope);
            reports.statusCounts(scope);
        }
        List<Long> milliseconds = Collections.synchronizedList(new ArrayList<>());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = new ArrayList<Future<?>>();
            for (int user = 0; user < 10; user++) {
                tasks.add(
                        executor.submit(
                                () -> {
                                    for (int repeat = 0; repeat < 20; repeat++) {
                                        long started = System.nanoTime();
                                        assertEquals(20, applications.list(scope).size());
                                        assertEquals(
                                                5000,
                                                reports.statusCounts(scope).stream()
                                                        .mapToLong(
                                                                row ->
                                                                        ((Number) row.get("total"))
                                                                                .longValue())
                                                        .sum());
                                        milliseconds.add((System.nanoTime() - started) / 1_000_000);
                                    }
                                }));
            }
            for (var task : tasks) {
                task.get();
            }
        }
        var sorted = new ArrayList<>(milliseconds);
        Collections.sort(sorted);
        long p95 = sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
        var result = new HashMap<String, Object>();
        result.put("scope", "로컬 MariaDB, JDBC/MyBatis 목록+집계 한 쌍. HTTP·운영 성능 아님");
        result.put("database", jdbc.queryForObject("SELECT VERSION()", String.class));
        result.put("rows", 10000);
        result.put("assignedRows", 5000);
        result.put("concurrentTasks", 10);
        result.put("samples", sorted.size());
        result.put("p95Milliseconds", p95);
        result.put("maxMilliseconds", sorted.getLast());
        result.put("planBefore", before);
        result.put("planAfter", after);
        Files.createDirectories(Path.of("output"));
        Files.writeString(Path.of("output/sql-performance.json"), json.write(result));
        assertTrue(p95 < 1000, "로컬 SQL 목표 p95 1초를 초과했습니다: " + p95);
    }
}
