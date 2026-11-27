package com.algoprep.bootstrap.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import com.algoprep.aireview.infrastructure.job.AiBudgetRolloverJob;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
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
 * F5-3b admin token budget, rate limits and model prices (endpoints 18 to 26) and JOB-AI-03 on real
 * PostgreSQL and Redis with real JWT and the real F1 audit use case. The clock is the fixed test
 * clock of the Solution Review flow test (2026-11-09 10:30Z, which is 17:30 UTC+7 in November).
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(SolutionReviewFlowTest.Doubles.class)
class BudgetAdminFlowTest {
  private static final String BASE = "/api/v1/ai/admin";
  private static final String BUDGET = BASE + "/token-budget";
  private static final String LIMITS = BASE + "/limits-config";
  private static final String PRICES = BASE + "/model-prices";
  private static final String REVIEW =
      """
      {"approachTitle":"Hash map","verdict":"IMPROVABLE",
       "complexity":{"actualTime":"O(n)","optimalTime":"O(1)","timeExplanation":"one pass",
       "actualSpace":"O(n)","optimalSpace":"O(1)","spaceExplanation":"a map"},
       "strengths":["clear"],"improvements":["less memory"],"edgeCases":["empty"],
       "followUpTopics":["hashing"],"readabilityScore":3,
       "codeDiff":{"suggestedCode":"print(2)","explanation":"shorter"}}
      """;

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

  @Autowired WebApplicationContext context;
  @Autowired AccessTokenPort tokens;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @Autowired AiBudgetRolloverJob job;
  @Autowired SolutionReviewFlowTest.ScriptedLlm llm;
  @Autowired SolutionReviewFlowTest.ScriptedSubmissions submissions;
  private MockMvc mvc;
  private String admin;
  private UUID adminId;

  @BeforeEach
  void reset() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("TRUNCATE ai.ai_cache_entries, ai.solution_reviews, ai.token_usage CASCADE");
    jdbc.update("DELETE FROM ai.ai_model_prices");
    jdbc.update("DELETE FROM identity.system_audit_logs WHERE action_type LIKE 'AI_%'");
    jdbc.update(
        "UPDATE ai.ai_token_budget_configs SET limit_tokens = 0, locked_at = NULL,"
            + " period_start = DATE '2026-11-01', review_per_hour = 10, interview_per_hour = 3,"
            + " cooldown_seconds = 20, warn_percent = 70, updated_by = NULL WHERE id = 1");
    llm.script.clear();
    llm.calls.clear();
    submissions.accepted.clear();
    grants("AI_CONFIG", "ADMIN", true, true);
    grants("AI_TOKEN_BUDGET", "ADMIN", true, true);
    grants("AI_CONFIG", "STUDENT", false, false);
    grants("AI_TOKEN_BUDGET", "STUDENT", false, false);
    adminId = UUID.randomUUID();
    admin = token(adminId, "ADMIN", BaseCategory.ADMIN);
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

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,"
            + " 'Budget actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  /** Fresh admin token after the matrix cells were changed (permission version trap). */
  private String adminWith(
      boolean budgetRead, boolean budgetUpdate, boolean cfgRead, boolean cfgUpdate) {
    grants("AI_TOKEN_BUDGET", "ADMIN", budgetRead, budgetUpdate);
    grants("AI_CONFIG", "ADMIN", cfgRead, cfgUpdate);
    return token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
  }

  private MockHttpServletResponse call(MockHttpServletRequestBuilder request, String bearer)
      throws Exception {
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private MockHttpServletResponse withBody(
      MockHttpServletRequestBuilder request, String body, String bearer) throws Exception {
    return call(request.contentType(MediaType.APPLICATION_JSON).content(body), bearer);
  }

  private MockHttpServletResponse putLimit(String body, String bearer) throws Exception {
    return withBody(put(BUDGET + "/limit"), body, bearer);
  }

  private MockHttpServletResponse unlock(String bearer) throws Exception {
    return call(post(BUDGET + "/unlock"), bearer);
  }

  private MockHttpServletResponse patchLimits(String body, String bearer) throws Exception {
    return withBody(patch(LIMITS), body, bearer);
  }

  private MockHttpServletResponse addPrice(String name, String bearer) throws Exception {
    return withBody(post(PRICES), "{\"modelName\":\"" + name + "\"}", bearer);
  }

  private long count(String sql) {
    return jdbc.queryForObject(sql, Long.class);
  }

  private long audits(String action) {
    return count(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type = '" + action + "'");
  }

  private long anyAiAudits() {
    return count("SELECT count(*) FROM identity.system_audit_logs WHERE action_type LIKE 'AI_%'");
  }

  private String code(MockHttpServletResponse response) throws Exception {
    return JsonPath.read(response.getContentAsString(), "$.errorCode");
  }

  private void expect(MockHttpServletResponse response, int status, String errorCode)
      throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    assertThat(code(response)).isEqualTo(errorCode);
  }

  private void usage(String feature, int tokensPrompt, int tokensCompletion, String at) {
    jdbc.update(
        "INSERT INTO ai.token_usage(id, call_id, user_id, feature_code, problem_id, owner_id,"
            + " tokens_prompt, tokens_completion, model_name, created_at)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'default', ?::timestamptz)",
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        feature,
        "SOLUTION_REVIEW".equals(feature) ? UUID.randomUUID() : null,
        UUID.randomUUID(),
        tokensPrompt,
        tokensCompletion,
        at);
  }

  private void setBudget(long limit, boolean locked) {
    jdbc.update(
        "UPDATE ai.ai_token_budget_configs SET limit_tokens = ?,"
            + " locked_at = CASE WHEN ? THEN now() END WHERE id = 1",
        limit,
        locked);
  }

  private Object dbLimit() {
    return jdbc.queryForObject(
        "SELECT limit_tokens FROM ai.ai_token_budget_configs WHERE id = 1", Long.class);
  }

  private boolean dbLocked() {
    return jdbc.queryForObject(
        "SELECT locked_at IS NOT NULL FROM ai.ai_token_budget_configs WHERE id = 1", Boolean.class);
  }

  private String dbPeriod() {
    return jdbc.queryForObject(
        "SELECT period_start::text FROM ai.ai_token_budget_configs WHERE id = 1", String.class);
  }

  private UUID submission(UUID owner, String code) {
    UUID id = UUID.randomUUID();
    submissions.accepted.put(
        id, new SubmissionSnapshot(id, owner, UUID.randomUUID(), "PYTHON", code));
    return id;
  }

  private MockHttpServletResponse review(UUID submission, String bearer) throws Exception {
    return call(post("/api/v1/ai/submissions/" + submission + "/solution-review"), bearer);
  }

  // Endpoint 18 -------------------------------------------------------------------------------

  @Test
  void statusShowsUsageBreakdownOfAllFourFeaturesAndALinearForecast() throws Exception {
    setBudget(1000, false);
    usage("SOLUTION_REVIEW", 200, 100, "2026-11-05T05:00:00Z");
    usage("PRACTICE_FEEDBACK", 60, 40, "2026-11-06T05:00:00Z");
    usage("MOCK_INTERVIEW", 5000, 5000, "2026-10-20T05:00:00Z"); // previous period: not counted
    var response = call(get(BUDGET), admin);
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat((Integer) JsonPath.read(body, "$.limitTokens")).isEqualTo(1000);
    assertThat((Integer) JsonPath.read(body, "$.usedTokens")).isEqualTo(400);
    assertThat((String) JsonPath.read(body, "$.periodStart")).isEqualTo("2026-11-01");
    assertThat((Boolean) JsonPath.read(body, "$.isLocked")).isFalse();
    assertThat((Object) JsonPath.read(body, "$.lockedAt")).isNull();
    assertThat((Integer) JsonPath.read(body, "$.warnPercent")).isEqualTo(70);
    assertThat((List<String>) JsonPath.read(body, "$.featureBreakdown[*].featureCode"))
        .containsExactly(
            "SOLUTION_REVIEW", "MOCK_INTERVIEW", "GENERATE_TESTCASE", "PRACTICE_FEEDBACK");
    assertThat((List<Integer>) JsonPath.read(body, "$.featureBreakdown[*].tokens"))
        .containsExactly(300, 0, 0, 100);
    // Window 11-03..11-09 is 7 days with 400 tokens; 600 left: 9 + ceil(600 * 7 / 400) = 11 days.
    assertThat((String) JsonPath.read(body, "$.projectedRunOutDate")).isEqualTo("2026-11-20");
  }

  @Test
  void anUnconfiguredLimitHasNoForecastAndDoesNotDivideByZero() throws Exception {
    usage("SOLUTION_REVIEW", 200, 100, "2026-11-05T05:00:00Z");
    String body = call(get(BUDGET), admin).getContentAsString();
    assertThat((Integer) JsonPath.read(body, "$.limitTokens")).isZero();
    assertThat((Boolean) JsonPath.read(body, "$.isLocked")).isFalse();
    assertThat((Object) JsonPath.read(body, "$.projectedRunOutDate")).isNull();
  }

  @Test
  void aReachedLimitReadsAsLockedEvenBeforeTheGateSetsLockedAt() throws Exception {
    setBudget(300, false);
    usage("SOLUTION_REVIEW", 200, 100, "2026-11-05T05:00:00Z");
    String body = call(get(BUDGET), admin).getContentAsString();
    assertThat((Boolean) JsonPath.read(body, "$.isLocked")).isTrue();
    assertThat((Object) JsonPath.read(body, "$.lockedAt")).isNull();
    assertThat((Object) JsonPath.read(body, "$.projectedRunOutDate")).isNull();
    assertThat(dbLocked()).isFalse();
  }

  @Test
  void aStoredLockShowsItsTimestampAndNoForecast() throws Exception {
    setBudget(1000, true);
    usage("SOLUTION_REVIEW", 100, 100, "2026-11-05T05:00:00Z");
    String body = call(get(BUDGET), admin).getContentAsString();
    assertThat((Boolean) JsonPath.read(body, "$.isLocked")).isTrue();
    assertThat((String) JsonPath.read(body, "$.lockedAt")).isNotBlank();
    assertThat((Object) JsonPath.read(body, "$.projectedRunOutDate")).isNull();
  }

  // Endpoint 19 -------------------------------------------------------------------------------

  @Test
  void settingTheLimitStoresItWithTheActorAndWritesOneAuditRow() throws Exception {
    var response = putLimit("{\"limitTokens\":5000000}", admin);
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    assertThat((Integer) JsonPath.read(response.getContentAsString(), "$.limitTokens"))
        .isEqualTo(5_000_000);
    assertThat(dbLimit()).isEqualTo(5_000_000L);
    assertThat(
            jdbc.queryForObject(
                "SELECT updated_by::text FROM ai.ai_token_budget_configs WHERE id = 1",
                String.class))
        .isEqualTo(adminId.toString());
    assertThat(audits("AI_BUDGET_LIMIT_CHANGED")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT actor_user_id::text FROM identity.system_audit_logs"
                    + " WHERE action_type = 'AI_BUDGET_LIMIT_CHANGED'",
                String.class))
        .isEqualTo(adminId.toString());
  }

  @Test
  void theLimitBoundariesAreOneAndNineTrillion() throws Exception {
    assertThat(putLimit("{\"limitTokens\":1}", admin).getStatus()).isEqualTo(200);
    assertThat(putLimit("{\"limitTokens\":9000000000000}", admin).getStatus()).isEqualTo(200);
    assertThat(dbLimit()).isEqualTo(9_000_000_000_000L);
    long audited = audits("AI_BUDGET_LIMIT_CHANGED");
    for (String bad :
        new String[] {
          "{\"limitTokens\":0}",
          "{\"limitTokens\":-1}",
          "{\"limitTokens\":1.5}",
          "{\"limitTokens\":\"5\"}",
          "{\"limitTokens\":null}",
          "{\"limitTokens\":9000000000001}",
          "{}",
          "not json"
        }) {
      expect(putLimit(bad, admin), 400, "AIR-V15");
    }
    assertThat(dbLimit()).isEqualTo(9_000_000_000_000L);
    assertThat(audits("AI_BUDGET_LIMIT_CHANGED")).isEqualTo(audited);
  }

  @Test
  void loweringTheLimitBelowUsageLocksOnlyAtTheNextAiCallOfAStudent() throws Exception {
    usage("SOLUTION_REVIEW", 200, 100, "2026-11-05T05:00:00Z");
    assertThat(putLimit("{\"limitTokens\":100}", admin).getStatus()).isEqualTo(200);
    assertThat(dbLocked()).isFalse();
    UUID user = UUID.randomUUID();
    String student = token(user, "STUDENT", BaseCategory.STUDENT);
    var rejected = review(submission(user, "print(1)"), student);
    expect(rejected, 429, "AIR-401");
    assertThat(dbLocked()).isTrue();
    assertThat(llm.calls).isEmpty();
  }

  @Test
  void raisingTheLimitOfALockedBudgetDoesNotUnlockIt() throws Exception {
    setBudget(100, true);
    usage("SOLUTION_REVIEW", 50, 50, "2026-11-05T05:00:00Z");
    var response = putLimit("{\"limitTokens\":1000}", admin);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat((Boolean) JsonPath.read(response.getContentAsString(), "$.isLocked")).isTrue();
    assertThat(dbLocked()).isTrue();
  }

  // Endpoint 20 -------------------------------------------------------------------------------

  @Test
  void unlockWithRoomLeftClearsTheLockAuditsOnceAndTheStudentCanCallAiAgain() throws Exception {
    setBudget(1000, true);
    usage("SOLUTION_REVIEW", 100, 100, "2026-11-05T05:00:00Z");
    var response = unlock(admin);
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
    assertThat((Object) JsonPath.read(response.getContentAsString(), "$.lockedAt")).isNull();
    assertThat((Boolean) JsonPath.read(response.getContentAsString(), "$.isLocked")).isFalse();
    assertThat(dbLocked()).isFalse();
    assertThat(audits("AI_BUDGET_UNLOCKED")).isEqualTo(1);

    UUID user = UUID.randomUUID();
    String student = token(user, "STUDENT", BaseCategory.STUDENT);
    llm.ok(REVIEW);
    assertThat(review(submission(user, "print(1)"), student).getStatus()).isEqualTo(200);
  }

  @Test
  void unlockIsRefusedWhileUsageHasReachedTheLimitAndNothingChanges() throws Exception {
    setBudget(300, true);
    usage("SOLUTION_REVIEW", 200, 100, "2026-11-05T05:00:00Z");
    expect(unlock(admin), 422, "AIR-203");
    assertThat(dbLocked()).isTrue();
    assertThat(anyAiAudits()).isZero();
    // One token of room left: the boundary opens.
    setBudget(301, true);
    assertThat(unlock(admin).getStatus()).isEqualTo(200);
    assertThat(dbLocked()).isFalse();
  }

  @Test
  void unlockOfAnUnlockedBudgetIs200WithNoChangeAndNoAuditRow() throws Exception {
    setBudget(1000, false);
    var response = unlock(admin);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(dbLocked()).isFalse();
    assertThat(anyAiAudits()).isZero();
    assertThat(unlock(admin).getStatus()).isEqualTo(200);
    assertThat(anyAiAudits()).isZero();
  }

  @Test
  void aLockedBudgetWithoutAConfiguredLimitCanBeUnlocked() throws Exception {
    setBudget(0, true);
    assertThat(unlock(admin).getStatus()).isEqualTo(200);
    assertThat(dbLocked()).isFalse();
  }

  // Endpoints 21 and 22 -----------------------------------------------------------------------

  @Test
  void limitsShowTheSeededDefaults() throws Exception {
    String body = call(get(LIMITS), admin).getContentAsString();
    assertThat((Integer) JsonPath.read(body, "$.reviewPerHour")).isEqualTo(10);
    assertThat((Integer) JsonPath.read(body, "$.interviewPerHour")).isEqualTo(3);
    assertThat((Integer) JsonPath.read(body, "$.cooldownSeconds")).isEqualTo(20);
    assertThat((Integer) JsonPath.read(body, "$.warnPercent")).isEqualTo(70);
  }

  @Test
  void aPartialPatchChangesOnlyTheGivenFieldsAndIsIdempotent() throws Exception {
    for (int run = 0; run < 2; run++) {
      var response = patchLimits("{\"reviewPerHour\":5,\"unknown\":1}", admin);
      assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
      String body = response.getContentAsString();
      assertThat((Integer) JsonPath.read(body, "$.reviewPerHour")).isEqualTo(5);
      assertThat((Integer) JsonPath.read(body, "$.interviewPerHour")).isEqualTo(3);
      assertThat((Integer) JsonPath.read(body, "$.cooldownSeconds")).isEqualTo(20);
      assertThat((Integer) JsonPath.read(body, "$.warnPercent")).isEqualTo(70);
    }
    var all =
        patchLimits("{\"interviewPerHour\":999,\"cooldownSeconds\":1,\"warnPercent\":100}", admin);
    assertThat((Integer) JsonPath.read(all.getContentAsString(), "$.interviewPerHour"))
        .isEqualTo(999);
    assertThat(
            jdbc.queryForObject(
                "SELECT updated_by::text FROM ai.ai_token_budget_configs WHERE id = 1",
                String.class))
        .isEqualTo(adminId.toString());
    assertThat(anyAiAudits()).as("no admin audit for rate limits (DD Q9)").isZero();
  }

  @Test
  void limitsOutsideTheirRangesAreRejectedAndStoreNothing() throws Exception {
    for (String bad :
        new String[] {
          "{}",
          "{\"unknown\":1}",
          "{\"cooldownSeconds\":1000}",
          "{\"cooldownSeconds\":0}",
          "{\"reviewPerHour\":-1}",
          "{\"interviewPerHour\":1000}",
          "{\"warnPercent\":0}",
          "{\"warnPercent\":101}",
          "{\"warnPercent\":2.5}",
          "{\"reviewPerHour\":4,\"warnPercent\":101}",
          "garbage"
        }) {
      expect(patchLimits(bad, admin), 400, "AIR-V16");
    }
    String body = call(get(LIMITS), admin).getContentAsString();
    assertThat((Integer) JsonPath.read(body, "$.reviewPerHour")).isEqualTo(10);
    assertThat((Integer) JsonPath.read(body, "$.cooldownSeconds")).isEqualTo(20);
  }

  @Test
  void aLowerReviewQuotaAppliesToTheVeryNextCall() throws Exception {
    assertThat(patchLimits("{\"reviewPerHour\":2,\"cooldownSeconds\":1}", admin).getStatus())
        .isEqualTo(200);
    UUID user = UUID.randomUUID();
    String student = token(user, "STUDENT", BaseCategory.STUDENT);
    for (int i = 0; i < 2; i++) {
      redis.delete("ai:cooldown:" + user);
      llm.ok(REVIEW);
      var ok = review(submission(user, "print(" + i + ")"), student);
      assertThat(ok.getStatus()).as(ok.getContentAsString()).isEqualTo(200);
    }
    redis.delete("ai:cooldown:" + user);
    llm.ok(REVIEW);
    expect(review(submission(user, "print(99)"), student), 429, "AIR-402");
  }

  // Endpoints 23 to 26 ------------------------------------------------------------------------

  @Test
  void priceListStartsEmptyAndIsSortedIgnoringCase() throws Exception {
    String empty = call(get(PRICES), admin).getContentAsString();
    assertThat((List<Object>) JsonPath.read(empty, "$.items")).isEmpty();
    assertThat(addPrice("beta", admin).getStatus()).isEqualTo(201);
    assertThat(addPrice("Alpha", admin).getStatus()).isEqualTo(201);
    assertThat(addPrice("gamma", admin).getStatus()).isEqualTo(201);
    String body = call(get(PRICES), admin).getContentAsString();
    assertThat((List<String>) JsonPath.read(body, "$.items[*].modelName"))
        .containsExactly("Alpha", "beta", "gamma");
  }

  @Test
  void creatingAPriceTrimsTheNameStartsAtZeroAndRejectsDuplicatesIgnoringCase() throws Exception {
    var created = addPrice("  gpt-x  ", admin);
    assertThat(created.getStatus()).as(created.getContentAsString()).isEqualTo(201);
    String body = created.getContentAsString();
    assertThat((String) JsonPath.read(body, "$.modelName")).isEqualTo("gpt-x");
    assertThat(((Number) JsonPath.read(body, "$.usdPerMillionTokens")).doubleValue()).isZero();
    UUID.fromString(JsonPath.read(body, "$.id"));
    expect(addPrice("GPT-X", admin), 409, "AIR-204");
    assertThat(count("SELECT count(*) FROM ai.ai_model_prices")).isEqualTo(1);
    assertThat(anyAiAudits()).as("no admin audit for prices (DD Q9)").isZero();
  }

  @Test
  void theNameLengthBoundaryIsOneHundredCharacters() throws Exception {
    assertThat(addPrice("a".repeat(100), admin).getStatus()).isEqualTo(201);
    expect(addPrice("a".repeat(101), admin), 400, "AIR-V17");
    expect(addPrice("   ", admin), 400, "AIR-V17");
    expect(withBody(post(PRICES), "{}", admin), 400, "AIR-V17");
    expect(withBody(post(PRICES), "{\"modelName\":5}", admin), 400, "AIR-V17");
    expect(withBody(post(PRICES), "garbage", admin), 400, "AIR-V17");
    assertThat(count("SELECT count(*) FROM ai.ai_model_prices")).isEqualTo(1);
  }

  private String priceId(String name) {
    return jdbc.queryForObject(
        "SELECT id::text FROM ai.ai_model_prices WHERE model_name = ?", String.class, name);
  }

  @Test
  void updatingAPriceStoresItAndRenamesWithTheSameRules() throws Exception {
    addPrice("m1", admin);
    addPrice("m2", admin);
    String id = priceId("m1");
    var priced = withBody(patch(PRICES + "/" + id), "{\"usdPerMillionTokens\":0.15}", admin);
    assertThat(priced.getStatus()).as(priced.getContentAsString()).isEqualTo(200);
    assertThat(
            ((Number) JsonPath.read(priced.getContentAsString(), "$.usdPerMillionTokens"))
                .doubleValue())
        .isEqualTo(0.15);
    assertThat((String) JsonPath.read(priced.getContentAsString(), "$.modelName")).isEqualTo("m1");

    var renamed = withBody(patch(PRICES + "/" + id), "{\"modelName\":\"M1\"}", admin);
    assertThat(renamed.getStatus()).isEqualTo(200);
    assertThat(priceId("M1")).isEqualTo(id);
    assertThat(
            jdbc.queryForObject(
                "SELECT usd_per_million_tokens::text FROM ai.ai_model_prices WHERE id = ?::uuid",
                String.class,
                id))
        .isEqualTo("0.1500");
    expect(withBody(patch(PRICES + "/" + id), "{\"modelName\":\"M2\"}", admin), 409, "AIR-204");
    assertThat(
            withBody(
                    patch(PRICES + "/" + id),
                    "{\"modelName\":\"x\",\"usdPerMillionTokens\":999999.9999}",
                    admin)
                .getStatus())
        .isEqualTo(200);
  }

  @Test
  void invalidPriceEditsAreRejectedAndUnknownIdsAreNotFound() throws Exception {
    addPrice("m1", admin);
    String path = PRICES + "/" + priceId("m1");
    for (String bad :
        new String[] {
          "{\"usdPerMillionTokens\":-1}",
          "{\"usdPerMillionTokens\":\"abc\"}",
          "{\"usdPerMillionTokens\":0.00001}",
          "{\"usdPerMillionTokens\":1000000}"
        }) {
      expect(withBody(patch(path), bad, admin), 400, "AIR-V18");
    }
    expect(withBody(patch(path), "{}", admin), 400, "AIR-V17");
    expect(withBody(patch(path), "{\"modelName\":\" \"}", admin), 400, "AIR-V17");
    expect(
        withBody(patch(PRICES + "/" + UUID.randomUUID()), "{\"usdPerMillionTokens\":1}", admin),
        404,
        "AIR-121");
    expect(
        withBody(patch(PRICES + "/not-a-uuid"), "{\"usdPerMillionTokens\":1}", admin),
        404,
        "AIR-121");
    expect(call(delete(PRICES + "/not-a-uuid"), admin), 404, "AIR-121");
    assertThat(
            jdbc.queryForObject(
                "SELECT usd_per_million_tokens::text FROM ai.ai_model_prices WHERE model_name='m1'",
                String.class))
        .isEqualTo("0.0000");
  }

  @Test
  void deleteIsIdempotentAndIgnoresExistingUsageRows() throws Exception {
    addPrice("m1", admin);
    usage("PRACTICE_FEEDBACK", 10, 10, "2026-11-05T05:00:00Z");
    jdbc.update("UPDATE ai.token_usage SET model_name = 'm1'");
    String path = PRICES + "/" + priceId("m1");
    assertThat(call(delete(path), admin).getStatus()).isEqualTo(204);
    assertThat(call(delete(path), admin).getStatus()).isEqualTo(204);
    assertThat(call(delete(PRICES + "/" + UUID.randomUUID()), admin).getStatus()).isEqualTo(204);
    assertThat(count("SELECT count(*) FROM ai.ai_model_prices")).isZero();
    assertThat(count("SELECT count(*) FROM ai.token_usage")).isEqualTo(1);
  }

  // Permissions -------------------------------------------------------------------------------

  /**
   * Expected statuses in order: budgetRead, limitsRead, pricesRead, putLimit, unlock, patchLimits,
   * createPrice.
   */
  private void expectStatuses(String bearer, int... expected) throws Exception {
    assertThat(call(get(BUDGET), bearer).getStatus()).as("GET token-budget").isEqualTo(expected[0]);
    assertThat(call(get(LIMITS), bearer).getStatus())
        .as("GET limits-config")
        .isEqualTo(expected[1]);
    assertThat(call(get(PRICES), bearer).getStatus()).as("GET model-prices").isEqualTo(expected[2]);
    assertThat(putLimit("{\"limitTokens\":777}", bearer).getStatus())
        .as("PUT limit")
        .isEqualTo(expected[3]);
    assertThat(unlock(bearer).getStatus()).as("POST unlock").isEqualTo(expected[4]);
    assertThat(patchLimits("{\"reviewPerHour\":7}", bearer).getStatus())
        .as("PATCH limits-config")
        .isEqualTo(expected[5]);
    assertThat(addPrice("perm-" + UUID.randomUUID(), bearer).getStatus())
        .as("POST model-prices")
        .isEqualTo(expected[6]);
  }

  @Test
  void noTokenIs401() throws Exception {
    assertThat(call(get(BUDGET), null).getStatus()).isEqualTo(401);
    assertThat(unlock(null).getStatus()).isEqualTo(401);
    assertThat(call(get(PRICES), null).getStatus()).isEqualTo(401);
  }

  @Test
  void budgetReadAloneOpensBothBudgetReadsAndNothingElse() throws Exception {
    expectStatuses(adminWith(true, false, false, false), 200, 200, 403, 403, 403, 403, 403);
  }

  @Test
  void configReadAloneOpensBothBudgetReadsAndThePriceListNothingElse() throws Exception {
    expectStatuses(adminWith(false, false, true, false), 200, 200, 200, 403, 403, 403, 403);
  }

  @Test
  void configUpdateIsNotEnoughForTheBudgetLimitAndUnlock() throws Exception {
    String token = adminWith(false, false, false, true);
    expectStatuses(token, 403, 403, 403, 403, 403, 200, 201);
    assertThat(dbLimit()).isEqualTo(0L);
  }

  @Test
  void budgetUpdateOpensLimitAndUnlockButNotRateLimitsOrPrices() throws Exception {
    String token = adminWith(false, true, false, false);
    expectStatuses(token, 403, 403, 403, 200, 200, 403, 403);
    assertThat(dbLimit()).isEqualTo(777L);
  }

  @Test
  void aNonAdminWithEveryCellIsRefusedEverywhere() throws Exception {
    grants("AI_CONFIG", "STUDENT", true, true);
    grants("AI_TOKEN_BUDGET", "STUDENT", true, true);
    String student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
    expectStatuses(student, 403, 403, 403, 403, 403, 403, 403);
    assertThat(code(call(get(BUDGET), student))).isEqualTo("AIR-301");
    assertThat(dbLimit()).isEqualTo(0L);
    assertThat(count("SELECT count(*) FROM ai.ai_model_prices")).isZero();
  }

  // Audit failure -----------------------------------------------------------------------------

  @Test
  void anAuditFailureAnswers503AndRollsTheLimitAndUnlockBack() throws Exception {
    setBudget(1000, true);
    jdbc.execute(
        "CREATE FUNCTION identity.reject_ai_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
            + " IF NEW.action_type LIKE 'AI_%' THEN RAISE EXCEPTION 'test audit unavailable';"
            + " END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_ai_audit BEFORE INSERT ON identity.system_audit_logs FOR EACH ROW"
            + " EXECUTE FUNCTION identity.reject_ai_audit()");
    try {
      expect(putLimit("{\"limitTokens\":5000}", admin), 503, "AIR-503");
      assertThat(dbLimit()).isEqualTo(1000L);
      expect(unlock(admin), 503, "AIR-503");
      assertThat(dbLocked()).isTrue();
    } finally {
      jdbc.execute("DROP TRIGGER reject_ai_audit ON identity.system_audit_logs");
      jdbc.execute("DROP FUNCTION identity.reject_ai_audit()");
    }
    assertThat(putLimit("{\"limitTokens\":5000}", admin).getStatus()).isEqualTo(200);
    assertThat(unlock(admin).getStatus()).isEqualTo(200);
    assertThat(anyAiAudits()).isEqualTo(2);
  }

  // JOB-AI-03 ---------------------------------------------------------------------------------

  private void staleLockedPeriod() {
    jdbc.update(
        "UPDATE ai.ai_token_budget_configs SET period_start = DATE '2026-10-01', limit_tokens = 100,"
            + " locked_at = now(), updated_by = ?, updated_at = TIMESTAMPTZ '2026-10-31 00:00:00+00'"
            + " WHERE id = 1",
        adminId);
    usage("SOLUTION_REVIEW", 300, 200, "2026-10-20T05:00:00Z");
    usage("PRACTICE_FEEDBACK", 20, 10, "2026-11-09T01:00:00Z");
  }

  @Test
  void rolloverMovesThePeriodClearsTheLockAndTheOldUsageNoLongerCounts() throws Exception {
    staleLockedPeriod();
    assertThat(
            (Integer) JsonPath.read(call(get(BUDGET), admin).getContentAsString(), "$.usedTokens"))
        .isEqualTo(530);

    job.run();

    assertThat(dbPeriod()).isEqualTo("2026-11-01");
    assertThat(dbLocked()).isFalse();
    assertThat(
            jdbc.queryForObject(
                "SELECT updated_by FROM ai.ai_token_budget_configs WHERE id = 1", UUID.class))
        .as("the system acts, not an admin (DD jobs F5 section 3)")
        .isNull();
    String body = call(get(BUDGET), admin).getContentAsString();
    assertThat((Integer) JsonPath.read(body, "$.usedTokens")).isEqualTo(30);
    assertThat((Boolean) JsonPath.read(body, "$.isLocked")).isFalse();
    assertThat(anyAiAudits()).as("JOB-AI-03 writes only a log line").isZero();

    UUID user = UUID.randomUUID();
    String student = token(user, "STUDENT", BaseCategory.STUDENT);
    llm.ok(REVIEW);
    assertThat(review(submission(user, "print(1)"), student).getStatus()).isEqualTo(200);
  }

  @Test
  void aSecondRunInTheSameMonthChangesNothing() throws Exception {
    staleLockedPeriod();
    job.run();
    jdbc.update("UPDATE ai.ai_token_budget_configs SET locked_at = now() WHERE id = 1");
    String updatedAt =
        jdbc.queryForObject(
            "SELECT updated_at::text FROM ai.ai_token_budget_configs WHERE id = 1", String.class);
    job.run();
    assertThat(dbPeriod()).isEqualTo("2026-11-01");
    assertThat(dbLocked()).as("an admin lock set after the rollover survives").isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT updated_at::text FROM ai.ai_token_budget_configs WHERE id = 1",
                String.class))
        .isEqualTo(updatedAt);
  }

  @Test
  void aPeriodStartedInTheFutureIsNeverMovedBackwards() {
    jdbc.update(
        "UPDATE ai.ai_token_budget_configs SET period_start = DATE '2026-12-01' WHERE id = 1");
    job.run();
    assertThat(dbPeriod()).isEqualTo("2026-12-01");
  }

  @Test
  void theStartupEventRunsTheSameRolloverSoAMissedMonthBoundaryIsCaughtUp() {
    jdbc.update(
        "UPDATE ai.ai_token_budget_configs SET period_start = DATE '2026-09-01', locked_at = now()"
            + " WHERE id = 1");
    context.publishEvent(
        new ApplicationReadyEvent(
            new SpringApplication(),
            new String[0],
            (org.springframework.context.ConfigurableApplicationContext) context,
            Duration.ZERO));
    assertThat(dbPeriod())
        .as("jumps to the current month, not the next one")
        .isEqualTo("2026-11-01");
    assertThat(dbLocked()).isFalse();
  }

  @Test
  void theAdvisoryLockStopsAConcurrentInstanceFromRunning() throws Exception {
    staleLockedPeriod();
    try (Connection other = jdbc.getDataSource().getConnection()) {
      other
          .createStatement()
          .execute("SELECT pg_advisory_lock(hashtextextended('ai:budget:rollover', 0))");
      job.run();
      assertThat(dbPeriod()).as("lock held elsewhere: skipped").isEqualTo("2026-10-01");
      assertThat(dbLocked()).isTrue();
      other
          .createStatement()
          .execute("SELECT pg_advisory_unlock(hashtextextended('ai:budget:rollover', 0))");
    }
    job.run();
    assertThat(dbPeriod()).isEqualTo("2026-11-01");
    assertThat(dbLocked()).isFalse();
  }
}
