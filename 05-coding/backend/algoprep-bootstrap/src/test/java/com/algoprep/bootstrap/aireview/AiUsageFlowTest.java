package com.algoprep.bootstrap.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.aireview.application.ports.out.ProblemBankStatsPort;
import com.algoprep.aireview.application.ports.out.UserDirectoryPort;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.ProblemSummary;
import com.algoprep.aireview.infrastructure.job.AiUsageAnomalyJob;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * F5-4 admin usage statistics and anomaly alerts (endpoints 27 to 31) and JOB-AI-05 on real
 * PostgreSQL and Redis with real JWT and the real F1 user lookups. The clock is the fixed test
 * clock of the Solution Review flow test (2026-11-09 10:30Z, which is 17:30 UTC+7). Problem titles
 * and the failure switches come from scripted doubles; names and roles are the real F1 data.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
    })
@ActiveProfiles("test")
@Testcontainers
@Import({SolutionReviewFlowTest.Doubles.class, AiUsageFlowTest.UsageDoubles.class})
class AiUsageFlowTest {
  private static final String BASE = "/api/v1/ai/admin/usage";
  private static final String NOW_DAY_START = "2026-11-09";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.data.redis.timeout", () -> "2s");
  }

  /** Real F1 reads unless the switch is on; titles come from a map. */
  static final class ScriptedDirectory implements UserDirectoryPort {
    private final UserDirectoryPort real;
    volatile boolean fail;

    ScriptedDirectory(UserDirectoryPort real) {
      this.real = real;
    }

    @Override
    public Map<UUID, String> getDisplayNames(Collection<UUID> userIds) {
      if (fail) {
        throw new IllegalStateException("identity down");
      }
      return real.getDisplayNames(userIds);
    }

    @Override
    public Map<UUID, CallerCategory> getBaseCategories(Collection<UUID> userIds) {
      if (fail) {
        throw new IllegalStateException("identity down");
      }
      return real.getBaseCategories(userIds);
    }
  }

  static final class ScriptedStats implements ProblemBankStatsPort {
    final Map<UUID, ProblemSummary> titles = new HashMap<>();
    volatile boolean fail;

    @Override
    public List<ProblemSummary> problemSummaries(Set<UUID> problemIds) {
      if (fail) {
        throw new IllegalStateException("problem-bank down");
      }
      return problemIds.stream().map(titles::get).filter(java.util.Objects::nonNull).toList();
    }
  }

  @TestConfiguration
  static class UsageDoubles {
    @Bean
    @Primary
    ScriptedDirectory scriptedDirectory(AiUserDirectoryBridge real) {
      return new ScriptedDirectory(real);
    }

    @Bean
    @Primary
    ScriptedStats scriptedStats() {
      return new ScriptedStats();
    }
  }

  @Autowired WebApplicationContext context;
  @Autowired AccessTokenPort tokens;
  @Autowired JdbcTemplate jdbc;
  @Autowired AiUsageAnomalyJob job;
  @Autowired ScriptedDirectory directory;
  @Autowired ScriptedStats stats;
  private MockMvc mvc;
  private String admin;

  @BeforeEach
  void reset() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update(
        "TRUNCATE ai.ai_cache_entries, ai.solution_reviews, ai.token_usage,"
            + " ai.ai_usage_anomaly_alerts CASCADE");
    jdbc.update("DELETE FROM ai.ai_model_prices");
    jdbc.update("DELETE FROM identity.system_audit_logs WHERE action_type LIKE 'AI_%'");
    jdbc.update(
        "UPDATE ai.ai_token_budget_configs SET limit_tokens = 0, locked_at = NULL,"
            + " period_start = DATE '2026-11-01', warn_percent = 70, updated_by = NULL"
            + " WHERE id = 1");
    directory.fail = false;
    stats.fail = false;
    stats.titles.clear();
    grants("AI_CONFIG", "ADMIN", true, true);
    grants("AI_TOKEN_BUDGET", "ADMIN", true, true);
    grants("AI_CONFIG", "STUDENT", false, false);
    grants("AI_TOKEN_BUDGET", "STUDENT", false, false);
    admin = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN, "Admin actor");
  }

  // Helpers -----------------------------------------------------------------------------------

  private void grants(String function, String role, boolean read, boolean update) {
    cell(function, role, "READ", read);
    cell(function, role, "UPDATE", update);
  }

  private void cell(String function, String role, String action, boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted=? FROM identity.roles r, identity.functions f,"
            + " identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id"
            + " AND r.code=? AND f.code=? AND a.code=?",
        granted,
        role,
        function,
        action);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code=?", role);
  }

  private void user(UUID id, String role, String name) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,?,id,'ACTIVE'"
            + " FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        name,
        role);
  }

  private String token(UUID id, String role, BaseCategory category, String name) {
    user(id, role, name);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  /** Fresh admin token after the matrix cells were changed (permission version trap). */
  private String adminWith(boolean budgetRead, boolean budgetUpdate, boolean cfgRead) {
    grants("AI_TOKEN_BUDGET", "ADMIN", budgetRead, budgetUpdate);
    grants("AI_CONFIG", "ADMIN", cfgRead, cfgRead);
    return token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN, "Matrix admin");
  }

  private MockHttpServletResponse call(MockHttpServletRequestBuilder request, String bearer)
      throws Exception {
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private MockHttpServletResponse getAs(String path, String bearer) throws Exception {
    return call(get(BASE + path), bearer);
  }

  private MockHttpServletResponse reviewAs(String alertId, String bearer) throws Exception {
    return call(post(BASE + "/alerts/" + alertId + "/review"), bearer);
  }

  private static <T> T json(MockHttpServletResponse response, String path) throws Exception {
    return JsonPath.read(response.getContentAsString(), path);
  }

  private void expect(MockHttpServletResponse response, int status, String errorCode)
      throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    assertThat((String) json(response, "$.errorCode")).isEqualTo(errorCode);
  }

  private MockHttpServletResponse ok(MockHttpServletResponse response) throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    return response;
  }

  private record UsageRow(
      UUID user,
      String feature,
      UUID problem,
      String model,
      int prompt,
      int completion,
      String at) {}

  private void usage(UsageRow row) {
    UUID problemId = row.problem();
    if ("SOLUTION_REVIEW".equals(row.feature()) && problemId == null) {
      problemId = UUID.randomUUID();
    }
    UUID owner = "GENERATE_TESTCASE".equals(row.feature()) ? problemId : UUID.randomUUID();
    jdbc.update(
        "INSERT INTO ai.token_usage(id, call_id, user_id, feature_code, problem_id, owner_id,"
            + " tokens_prompt, tokens_completion, model_name, created_at)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::timestamptz)",
        UUID.randomUUID(),
        UUID.randomUUID(),
        row.user(),
        row.feature(),
        problemId,
        owner,
        row.prompt(),
        row.completion(),
        row.model(),
        row.at());
  }

  private void calls(UUID user, int count, String at) {
    for (int i = 0; i < count; i++) {
      usage(new UsageRow(user, "MOCK_INTERVIEW", null, "default", 1, 1, at));
    }
  }

  private void price(String model, String usdPerMillion) {
    jdbc.update(
        "INSERT INTO ai.ai_model_prices(id, model_name, usd_per_million_tokens)"
            + " VALUES (?, ?, ?::numeric)",
        UUID.randomUUID(),
        model,
        usdPerMillion);
  }

  private UUID alert(UUID user, String status, String createdAt, int calls) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO ai.ai_usage_anomaly_alerts(id, user_id, window_start, window_end, call_count,"
            + " baseline_avg, ratio, status, created_at, reviewed_at) VALUES (?, ?,"
            + " ?::timestamptz - interval '24 hours', ?::timestamptz, ?, 5.00, 6.00, ?,"
            + " ?::timestamptz, CASE WHEN ? = 'REVIEWED' THEN now() END)",
        id,
        user,
        createdAt,
        createdAt,
        calls,
        status,
        createdAt,
        status);
    return id;
  }

  private long count(String sql) {
    return jdbc.queryForObject(sql, Long.class);
  }

  private long alertRows(String status) {
    return count("SELECT count(*) FROM ai.ai_usage_anomaly_alerts WHERE status = '" + status + "'");
  }

  // Permissions -------------------------------------------------------------------------------

  private static final String[] READS = {"/daily", "/alerts", "/top-users", "/top-problems"};

  @Test
  void aStudentAndAnAnonymousCallerAreRefusedEverywhere() throws Exception {
    UUID alertId = alert(UUID.randomUUID(), "OPEN", "2026-11-09T09:00:00Z", 30);
    grants("AI_TOKEN_BUDGET", "STUDENT", true, true);
    String student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT, "Student");
    for (String path : READS) {
      assertThat(getAs(path, null).getStatus()).as(path + " no token").isEqualTo(401);
      expect(getAs(path, student), 403, "AIR-301");
    }
    assertThat(reviewAs(alertId.toString(), null).getStatus()).isEqualTo(401);
    expect(reviewAs(alertId.toString(), student), 403, "AIR-301");
    assertThat(alertRows("OPEN")).as("a denied call changes nothing").isEqualTo(1);
  }

  @Test
  void configCellsAreNotEnoughForAnyUsageEndpoint() throws Exception {
    UUID alertId = alert(UUID.randomUUID(), "OPEN", "2026-11-09T09:00:00Z", 30);
    String configOnly = adminWith(false, false, true);
    for (String path : READS) {
      expect(getAs(path, configOnly), 403, "AIR-301");
    }
    expect(reviewAs(alertId.toString(), configOnly), 403, "AIR-301");
    assertThat(alertRows("OPEN")).isEqualTo(1);
  }

  @Test
  void budgetReadOpensTheFourReadsButNotTheReview() throws Exception {
    UUID alertId = alert(UUID.randomUUID(), "OPEN", "2026-11-09T09:00:00Z", 30);
    String readOnly = adminWith(true, false, false);
    for (String path : READS) {
      ok(getAs(path, readOnly));
    }
    expect(reviewAs(alertId.toString(), readOnly), 403, "AIR-301");
    assertThat(alertRows("OPEN")).isEqualTo(1);
  }

  @Test
  void budgetUpdateAloneOpensOnlyTheReview() throws Exception {
    UUID alertId = alert(UUID.randomUUID(), "OPEN", "2026-11-09T09:00:00Z", 30);
    String updateOnly = adminWith(false, true, false);
    for (String path : READS) {
      expect(getAs(path, updateOnly), 403, "AIR-301");
    }
    ok(reviewAs(alertId.toString(), updateOnly));
    assertThat(alertRows("REVIEWED")).isEqualTo(1);
  }

  // Endpoint 27 -------------------------------------------------------------------------------

  @Test
  void dailyBucketsByUtcPlus7DaysWithADenseSeriesOldestFirst() throws Exception {
    UUID u = UUID.randomUUID();
    usage(
        new UsageRow(
            u, "SOLUTION_REVIEW", null, "m", 10, 5, "2026-11-08T16:30:00Z")); // 23:30 on Nov 8
    usage(
        new UsageRow(
            u, "SOLUTION_REVIEW", null, "m", 20, 0, "2026-11-08T17:30:00Z")); // 00:30 on Nov 9
    usage(new UsageRow(u, "MOCK_INTERVIEW", null, "m", 7, 3, "2026-11-09T05:00:00Z"));
    usage(
        new UsageRow(
            u, "SOLUTION_REVIEW", null, "m", 5, 0, "2026-11-09T16:59:59Z")); // 23:59:59 on Nov 9
    usage(
        new UsageRow(
            u, "SOLUTION_REVIEW", null, "m", 999, 0, "2026-11-09T17:00:00Z")); // Nov 10, out
    usage(
        new UsageRow(
            u, "SOLUTION_REVIEW", null, "m", 100, 0, "2026-10-26T16:00:00Z")); // before the 14 days
    MockHttpServletResponse response = ok(getAs("/daily", admin));
    assertThat((Integer) json(response, "$.rangeDays")).isEqualTo(14);
    assertThat((Integer) json(response, "$.points.length()")).isEqualTo(14);
    assertThat((String) json(response, "$.points[0].date")).isEqualTo("2026-10-27");
    assertThat((String) json(response, "$.points[13].date")).isEqualTo(NOW_DAY_START);
    assertThat((Integer) json(response, "$.points[12].tokensByFeature.SOLUTION_REVIEW"))
        .isEqualTo(15);
    assertThat((Integer) json(response, "$.points[13].tokensByFeature.SOLUTION_REVIEW"))
        .isEqualTo(25);
    assertThat((Integer) json(response, "$.points[13].tokensByFeature.MOCK_INTERVIEW"))
        .isEqualTo(10);
    assertThat((Integer) json(response, "$.points[5].tokensByFeature.PRACTICE_FEEDBACK")).isZero();
    assertThat((Integer) json(response, "$.points[5].tokensByFeature.GENERATE_TESTCASE")).isZero();
  }

  @Test
  void dailyHonoursTheRangeAndRejectsOtherValues() throws Exception {
    UUID u = UUID.randomUUID();
    usage(
        new UsageRow(
            u, "SOLUTION_REVIEW", null, "m", 100, 0, "2026-10-26T16:00:00Z")); // Oct 26, 23:00
    assertThat((Integer) json(ok(getAs("/daily?rangeDays=7", admin)), "$.points.length()"))
        .isEqualTo(7);
    MockHttpServletResponse thirty = ok(getAs("/daily?rangeDays=30", admin));
    assertThat((Integer) json(thirty, "$.points.length()")).isEqualTo(30);
    assertThat((Integer) json(thirty, "$.points[15].tokensByFeature.SOLUTION_REVIEW"))
        .as("Oct 26 is inside the 30 day range")
        .isEqualTo(100);
    for (String bad : new String[] {"10", "0", "abc", "-7", ""}) {
      expect(getAs("/daily?rangeDays=" + bad, admin), 400, "AIR-V19");
    }
  }

  // Endpoints 28 and 29 -----------------------------------------------------------------------

  @Test
  void alertsListOnlyOpenOnesNewestFirstWithRealNamesAndNullWhenIdentityFails() throws Exception {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    user(first, "STUDENT", "Nguyen An");
    user(second, "STUDENT", "Tran Binh");
    UUID older = alert(first, "OPEN", "2026-11-09T08:00:00Z", 30);
    UUID newer = alert(second, "OPEN", "2026-11-09T09:00:00Z", 40);
    alert(first, "REVIEWED", "2026-11-09T07:00:00Z", 50);
    MockHttpServletResponse response = ok(getAs("/alerts", admin));
    assertThat((Integer) json(response, "$.items.length()")).isEqualTo(2);
    assertThat((String) json(response, "$.items[0].alertId")).isEqualTo(newer.toString());
    assertThat((String) json(response, "$.items[1].alertId")).isEqualTo(older.toString());
    assertThat((String) json(response, "$.items[0].alertType")).isEqualTo("ANOMALY");
    assertThat((String) json(response, "$.items[0].status")).isEqualTo("OPEN");
    assertThat((String) json(response, "$.items[0].displayName")).isEqualTo("Tran Binh");
    assertThat((Integer) json(response, "$.items[0].callCount")).isEqualTo(40);
    assertThat((String) json(response, "$.items[0].userId")).isEqualTo(second.toString());
    directory.fail = true;
    MockHttpServletResponse degraded = ok(getAs("/alerts", admin));
    assertThat((Integer) json(degraded, "$.items.length()")).isEqualTo(2);
    assertThat((Object) json(degraded, "$.items[0].displayName")).isNull();
  }

  @Test
  void aBudgetCardLeadsTheListFromTheWarnPercentAndNeverWithoutALimit() throws Exception {
    alert(UUID.randomUUID(), "OPEN", "2026-11-09T09:00:00Z", 30);
    usage(
        new UsageRow(
            UUID.randomUUID(), "SOLUTION_REVIEW", null, "m", 600, 100, "2026-11-05T05:00:00Z"));
    jdbc.update("UPDATE ai.ai_token_budget_configs SET limit_tokens = 1000 WHERE id = 1");
    MockHttpServletResponse response = ok(getAs("/alerts", admin));
    assertThat((Integer) json(response, "$.items.length()")).isEqualTo(2);
    assertThat((String) json(response, "$.items[0].alertType")).isEqualTo("BUDGET");
    assertThat((Object) json(response, "$.items[0].alertId")).isNull();
    assertThat((Object) json(response, "$.items[0].status")).isNull();
    assertThat(((Number) json(response, "$.items[0].usedPercent")).doubleValue()).isEqualTo(70.0);
    assertThat((String) json(response, "$.items[1].alertType")).isEqualTo("ANOMALY");
    jdbc.update("UPDATE ai.ai_token_budget_configs SET limit_tokens = 1001 WHERE id = 1");
    assertThat((Integer) json(ok(getAs("/alerts", admin)), "$.items.length()")).isEqualTo(1);
    jdbc.update("UPDATE ai.ai_token_budget_configs SET limit_tokens = 0 WHERE id = 1");
    assertThat((Integer) json(ok(getAs("/alerts", admin)), "$.items.length()"))
        .as("limit 0 means not configured")
        .isEqualTo(1);
  }

  @Test
  void markingReviewedIsIdempotentWritesNoAuditAndLeavesTheAccountAlone() throws Exception {
    UUID flagged = UUID.randomUUID();
    user(flagged, "STUDENT", "Flagged");
    UUID id = alert(flagged, "OPEN", "2026-11-09T09:00:00Z", 30);
    MockHttpServletResponse first = ok(reviewAs(id.toString(), admin));
    assertThat((String) json(first, "$.status")).isEqualTo("REVIEWED");
    assertThat((Boolean) json(first, "$.alreadyReviewed")).isFalse();
    assertThat((String) json(first, "$.alertId")).isEqualTo(id.toString());
    String reviewedAt = json(first, "$.reviewedAt");
    assertThat(reviewedAt).isNotBlank();
    MockHttpServletResponse second = ok(reviewAs(id.toString(), admin));
    assertThat((Boolean) json(second, "$.alreadyReviewed")).isTrue();
    assertThat((String) json(second, "$.reviewedAt")).isEqualTo(reviewedAt);
    assertThat(alertRows("REVIEWED")).isEqualTo(1);
    assertThat(
            count("SELECT count(*) FROM identity.system_audit_logs WHERE action_type LIKE 'AI_%'"))
        .as("Q9: marking an alert is not audited")
        .isZero();
    assertThat(count("SELECT count(*) FROM ai.ai_token_budget_configs WHERE locked_at IS NOT NULL"))
        .isZero();
    assertThat((Integer) json(ok(getAs("/alerts", admin)), "$.items.length()")).isZero();
  }

  @Test
  void anUnknownOrMalformedAlertIdIsAir120() throws Exception {
    expect(reviewAs(UUID.randomUUID().toString(), admin), 404, "AIR-120");
    expect(reviewAs("not-a-uuid", admin), 404, "AIR-120");
  }

  // Endpoint 30 -------------------------------------------------------------------------------

  @Test
  void topUsersAreRankedByTokensWithCostFromPricesAndNullWhenAModelHasNone() throws Exception {
    price("gpt-x", "0.15");
    price("Other", "2");
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    UUID e = UUID.randomUUID();
    UUID f = UUID.randomUUID();
    UUID d = UUID.randomUUID();
    user(a, "STUDENT", "Nguyen An");
    String inside = "2026-10-10T11:00:00Z"; // just inside the 30 days before 2026-11-09 10:30Z
    usage(
        new UsageRow(
            a,
            "SOLUTION_REVIEW",
            null,
            "GPT-X",
            600_000,
            400_000,
            inside)); // case-insensitive join
    usage(new UsageRow(a, "MOCK_INTERVIEW", null, "other", 500_000, 0, "2026-11-01T00:00:00Z"));
    usage(new UsageRow(b, "MOCK_INTERVIEW", null, "unpriced", 800_000, 0, inside));
    usage(new UsageRow(e, "MOCK_INTERVIEW", null, "gpt-x", 333_333, 0, inside));
    usage(new UsageRow(f, "MOCK_INTERVIEW", null, "gpt-x", 100_000, 0, inside));
    usage(new UsageRow(f, "MOCK_INTERVIEW", null, "unpriced", 100_000, 0, inside));
    usage(
        new UsageRow(
            d, "MOCK_INTERVIEW", null, "gpt-x", 9_000_000, 0, "2026-10-09T10:00:00Z")); // 31 days
    MockHttpServletResponse response = ok(getAs("/top-users", admin));
    assertThat((Integer) json(response, "$.items.length()")).isEqualTo(4);
    assertThat((String) json(response, "$.items[0].userId")).isEqualTo(a.toString());
    assertThat((String) json(response, "$.items[0].displayName")).isEqualTo("Nguyen An");
    assertThat(response.getContentAsString()).doesNotContain("userMeta");
    assertThat(((Number) json(response, "$.items[0].tokens")).longValue()).isEqualTo(1_500_000L);
    assertThat((Integer) json(response, "$.items[0].calls")).isEqualTo(2);
    assertThat(((Number) json(response, "$.items[0].costUsd")).doubleValue()).isEqualTo(1.15);
    assertThat((String) json(response, "$.items[1].userId")).isEqualTo(b.toString());
    assertThat((Object) json(response, "$.items[1].costUsd")).as("no price row").isNull();
    assertThat((String) json(response, "$.items[2].userId")).isEqualTo(e.toString());
    assertThat(((Number) json(response, "$.items[2].costUsd")).doubleValue())
        .as("0.04999995 rounds to 4 decimals")
        .isEqualTo(0.05);
    assertThat((String) json(response, "$.items[3].userId")).isEqualTo(f.toString());
    assertThat((Object) json(response, "$.items[3].costUsd"))
        .as("one unpriced model makes the whole cost unknown")
        .isNull();
  }

  @Test
  void topUsersLimitTiesAndNamesDegrade() throws Exception {
    price("gpt-x", "1");
    UUID x1 = UUID.randomUUID();
    UUID x2 = UUID.randomUUID();
    UUID big = UUID.randomUUID();
    usage(new UsageRow(x1, "MOCK_INTERVIEW", null, "gpt-x", 50_000, 0, "2026-11-01T00:00:00Z"));
    usage(new UsageRow(x2, "MOCK_INTERVIEW", null, "gpt-x", 50_000, 0, "2026-11-01T00:00:00Z"));
    usage(new UsageRow(big, "MOCK_INTERVIEW", null, "gpt-x", 90_000, 0, "2026-11-01T00:00:00Z"));
    String lower = x1.toString().compareTo(x2.toString()) < 0 ? x1.toString() : x2.toString();
    MockHttpServletResponse limited = ok(getAs("/top-users?limit=2", admin));
    assertThat((Integer) json(limited, "$.items.length()")).isEqualTo(2);
    assertThat((String) json(limited, "$.items[0].userId")).isEqualTo(big.toString());
    assertThat((String) json(limited, "$.items[1].userId"))
        .as("a tie breaks by user id")
        .isEqualTo(lower);
    directory.fail = true;
    MockHttpServletResponse degraded = ok(getAs("/top-users", admin));
    assertThat((Integer) json(degraded, "$.items.length()")).isEqualTo(3);
    assertThat((Object) json(degraded, "$.items[0].displayName")).isNull();
    assertThat(((Number) json(degraded, "$.items[0].tokens")).longValue()).isEqualTo(90_000L);
    for (String bad : new String[] {"0", "21", "-1", "x", ""}) {
      expect(getAs("/top-users?limit=" + bad, admin), 400, "AIR-V02");
    }
    ok(getAs("/top-users?limit=20", admin));
    ok(getAs("/top-users?limit=1", admin));
  }

  // Endpoint 31 -------------------------------------------------------------------------------

  @Test
  void topProblemsGroupByProblemSkipInterviewsAndRoundTheAverage() throws Exception {
    UUID p1 = UUID.randomUUID();
    UUID p2 = UUID.randomUUID();
    UUID p3 = UUID.randomUUID();
    UUID u = UUID.randomUUID();
    stats.titles.put(p1, new ProblemSummary(p1, "Two Sum", "EASY", "De"));
    stats.titles.put(p2, new ProblemSummary(p2, "LRU Cache", "MEDIUM", "Trung binh"));
    String at = "2026-11-01T00:00:00Z";
    usage(new UsageRow(u, "SOLUTION_REVIEW", p1, "m", 600, 400, at));
    usage(new UsageRow(u, "SOLUTION_REVIEW", p1, "m", 100, 0, at));
    usage(new UsageRow(u, "GENERATE_TESTCASE", p1, "m", 100, 0, at));
    usage(new UsageRow(u, "SOLUTION_REVIEW", p2, "m", 300, 0, at));
    usage(new UsageRow(u, "MOCK_INTERVIEW", null, "m", 5000, 0, at));
    usage(
        new UsageRow(
            u, "SOLUTION_REVIEW", p3, "m", 9000, 0, "2026-10-09T10:00:00Z")); // 31 days ago
    MockHttpServletResponse response = ok(getAs("/top-problems", admin));
    assertThat((Integer) json(response, "$.items.length()")).isEqualTo(2);
    assertThat((String) json(response, "$.items[0].problemId")).isEqualTo(p1.toString());
    assertThat((String) json(response, "$.items[0].problemTitle")).isEqualTo("Two Sum");
    assertThat((String) json(response, "$.items[0].levelCode")).isEqualTo("EASY");
    assertThat((String) json(response, "$.items[0].levelDisplayName")).isEqualTo("De");
    assertThat(((Number) json(response, "$.items[0].tokens")).longValue()).isEqualTo(1200L);
    assertThat((Integer) json(response, "$.items[0].calls")).isEqualTo(3);
    assertThat((Integer) json(response, "$.items[0].avgTokensPerCall")).isEqualTo(400);
    assertThat((Integer) json(response, "$.items[1].avgTokensPerCall")).isEqualTo(300);
    assertThat((Integer) json(ok(getAs("/top-problems?limit=1", admin)), "$.items.length()"))
        .isEqualTo(1);
    expect(getAs("/top-problems?limit=21", admin), 400, "AIR-V02");
    expect(getAs("/top-problems?limit=0", admin), 400, "AIR-V02");
  }

  @Test
  void topProblemsKeepTheRowsWithNullTitlesWhenProblemBankFails() throws Exception {
    UUID p1 = UUID.randomUUID();
    usage(
        new UsageRow(
            UUID.randomUUID(), "SOLUTION_REVIEW", p1, "m", 100, 0, "2026-11-01T00:00:00Z"));
    usage(
        new UsageRow(
            UUID.randomUUID(), "SOLUTION_REVIEW", p1, "m", 101, 0, "2026-11-01T00:00:00Z"));
    stats.fail = true;
    MockHttpServletResponse response = ok(getAs("/top-problems", admin));
    assertThat((String) json(response, "$.items[0].problemId")).isEqualTo(p1.toString());
    assertThat((Object) json(response, "$.items[0].problemTitle")).isNull();
    assertThat((Object) json(response, "$.items[0].levelCode")).isNull();
    assertThat((Integer) json(response, "$.items[0].avgTokensPerCall")).isEqualTo(101);
  }

  // JOB-AI-05 ---------------------------------------------------------------------------------

  private static final String RECENT = "2026-11-09T09:00:00Z";

  /** A heavy student, a quiet admin and four one-call users: average exactly 5.00. */
  private UUID heavyStudentWithAdminInTheAverage(int heavyCalls) {
    UUID heavy = UUID.randomUUID();
    user(heavy, "STUDENT", "Heavy student");
    UUID quietAdmin = UUID.randomUUID();
    user(quietAdmin, "ADMIN", "Quiet admin");
    calls(heavy, heavyCalls, RECENT);
    calls(quietAdmin, 1, RECENT);
    for (int i = 0; i < 4; i++) {
      calls(UUID.randomUUID(), 1, RECENT);
    }
    return heavy;
  }

  @Test
  void anAccountAtFiveTimesTheAverageGetsExactlyOneAlertAndTheAdminCountsInTheAverage() {
    UUID heavy = heavyStudentWithAdminInTheAverage(25);
    calls(heavy, 100, "2026-11-08T09:00:00Z"); // older than 24 hours: not counted
    job.run();
    assertThat(alertRows("OPEN")).isEqualTo(1);
    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT user_id, call_count, baseline_avg, ratio, status,"
                + " window_end - window_start = interval '24 hours' AS span FROM ai.ai_usage_anomaly_alerts");
    assertThat(row.get("user_id")).isEqualTo(heavy);
    assertThat(row.get("call_count")).isEqualTo(25);
    assertThat(row.get("baseline_avg").toString()).isEqualTo("5.00");
    assertThat(row.get("ratio").toString()).isEqualTo("5.00");
    assertThat(row.get("status")).isEqualTo("OPEN");
    assertThat(row.get("span")).as("window is the last 24 hours").isEqualTo(true);
    assertThat(count("SELECT count(*) FROM ai.ai_token_budget_configs WHERE locked_at IS NOT NULL"))
        .as("alerts never lock anything")
        .isZero();
  }

  @Test
  void justBelowFiveTimesTheAverageWritesNothing() {
    heavyStudentWithAdminInTheAverage(24);
    job.run();
    assertThat(count("SELECT count(*) FROM ai.ai_usage_anomaly_alerts")).isZero();
  }

  @Test
  void aSecondRunInTheSameHourDoesNotDuplicateAndAReviewedAlertAllowsANewOne() throws Exception {
    UUID heavy = heavyStudentWithAdminInTheAverage(25);
    job.run();
    job.run();
    assertThat(alertRows("OPEN")).as("one OPEN row per account").isEqualTo(1);
    String id =
        jdbc.queryForObject("SELECT id::text FROM ai.ai_usage_anomaly_alerts", String.class);
    ok(reviewAs(id, admin));
    job.run();
    assertThat(alertRows("REVIEWED")).isEqualTo(1);
    assertThat(alertRows("OPEN")).as("a fresh OPEN row after the review").isEqualTo(1);
    assertThat(
            count(
                "SELECT count(*) FROM ai.ai_usage_anomaly_alerts WHERE user_id = '" + heavy + "'"))
        .isEqualTo(2);
  }

  @Test
  void anAdminIsNeverAlertedEvenWhenHeavy() {
    UUID heavyAdmin = UUID.randomUUID();
    user(heavyAdmin, "ADMIN", "Heavy admin");
    calls(heavyAdmin, 25, RECENT);
    for (int i = 0; i < 5; i++) {
      calls(UUID.randomUUID(), 1, RECENT);
    }
    job.run();
    assertThat(count("SELECT count(*) FROM ai.ai_usage_anomaly_alerts")).isZero();
  }

  @Test
  void aRoleLookupFailureSkipsTheRunAndTheNextRunHeals() {
    heavyStudentWithAdminInTheAverage(25);
    directory.fail = true;
    job.run();
    assertThat(count("SELECT count(*) FROM ai.ai_usage_anomaly_alerts")).isZero();
    directory.fail = false;
    job.run();
    assertThat(alertRows("OPEN")).isEqualTo(1);
  }

  @Test
  void theAdvisoryLockStopsAConcurrentInstanceFromRunning() throws Exception {
    heavyStudentWithAdminInTheAverage(25);
    try (Connection other = jdbc.getDataSource().getConnection()) {
      other
          .createStatement()
          .execute("SELECT pg_advisory_lock(hashtextextended('ai:usage:anomaly', 0))");
      job.run();
      assertThat(count("SELECT count(*) FROM ai.ai_usage_anomaly_alerts"))
          .as("lock held elsewhere: skipped")
          .isZero();
      other
          .createStatement()
          .execute("SELECT pg_advisory_unlock(hashtextextended('ai:usage:anomaly', 0))");
    }
    job.run();
    assertThat(alertRows("OPEN")).isEqualTo(1);
  }

  @Test
  void anIdleSystemWritesNothing() {
    job.run();
    assertThat(count("SELECT count(*) FROM ai.ai_usage_anomaly_alerts")).isZero();
  }
}
