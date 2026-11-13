package com.algoprep.bootstrap.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 * F5-3a admin prompt templates (endpoints 14 to 17) on real PostgreSQL and Redis with real JWT and
 * the real F1 audit use case. Covers AC-04 (cache invalidation by prompt version, with the scripted
 * LLM of the Solution Review flow test) and the admin prompt ACs listed in the slice report.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "algoprep.ai.models.allowed=model-a,model-b"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(SolutionReviewFlowTest.Doubles.class)
class PromptTemplateFlowTest {
  private static final tools.jackson.databind.json.JsonMapper JSON =
      tools.jackson.databind.json.JsonMapper.builder().build();
  private static final String BASE = "/api/v1/ai/admin/prompt-templates";
  private static final String[] SEED = {
    "50000000-0000-4000-8000-000000000001",
    "50000000-0000-4000-8000-000000000002",
    "50000000-0000-4000-8000-000000000003"
  };
  private static final String GUARDS =
      "{\"citeLines\":true,\"answerInVietnamese\":true,\"noFullSolution\":true}";
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
  @Autowired SolutionReviewFlowTest.ScriptedLlm llm;
  @Autowired SolutionReviewFlowTest.ScriptedSubmissions submissions;
  private MockMvc mvc;
  private String admin;
  private UUID adminId;

  @BeforeEach
  void reset() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("TRUNCATE ai.ai_cache_entries, ai.solution_reviews, ai.token_usage CASCADE");
    jdbc.update(
        "DELETE FROM ai.prompt_templates WHERE id NOT IN (?::uuid, ?::uuid, ?::uuid)",
        SEED[0],
        SEED[1],
        SEED[2]);
    jdbc.update(
        "UPDATE ai.prompt_templates SET status = 'ACTIVE', published_at = COALESCE(published_at, now())");
    jdbc.update("DELETE FROM identity.system_audit_logs WHERE action_type LIKE 'AI_%'");
    jdbc.update(
        "UPDATE ai.ai_token_budget_configs SET limit_tokens = 0, locked_at = NULL,"
            + " period_start = DATE '2026-11-01', review_per_hour = 10, cooldown_seconds = 20"
            + " WHERE id = 1");
    llm.script.clear();
    llm.calls.clear();
    submissions.accepted.clear();
    grants("AI_CONFIG", "ADMIN", true, true);
    grants("AI_CONFIG", "STUDENT", false, false);
    grants("AI_TOKEN_BUDGET", "ADMIN", false, false);
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
            + " 'Prompt actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private MockHttpServletResponse call(MockHttpServletRequestBuilder request, String bearer)
      throws Exception {
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private MockHttpServletResponse draft(String feature, String body, String bearer)
      throws Exception {
    return call(
        put(BASE + "/" + feature + "/draft").contentType(MediaType.APPLICATION_JSON).content(body),
        bearer);
  }

  private MockHttpServletResponse publish(String body, String bearer) throws Exception {
    return call(
        post(BASE + "/publish").contentType(MediaType.APPLICATION_JSON).content(body), bearer);
  }

  private static String draftBody(String instruction, String model, String guards) {
    return "{\"systemInstruction\":\""
        + instruction
        + "\",\"modelId\":\""
        + model
        + "\",\"responseGuards\":"
        + guards
        + "}";
  }

  private static String change(String feature, String expectedId, String extra) {
    return "{\"featureCode\":\""
        + feature
        + "\",\"expectedActivePromptId\":\""
        + expectedId
        + "\""
        + extra
        + "}";
  }

  private static String publishBody(String... changes) {
    return "{\"changes\":[" + String.join(",", changes) + "]}";
  }

  private String activeId(String feature) {
    return jdbc.queryForObject(
        "SELECT id::text FROM ai.prompt_templates WHERE feature_code=? AND status='ACTIVE'",
        String.class,
        feature);
  }

  private long count(String sql) {
    return jdbc.queryForObject(sql, Long.class);
  }

  private long audits() {
    return count(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type = 'AI_PROMPT_PUBLISHED'");
  }

  private String code(MockHttpServletResponse response) throws Exception {
    return JsonPath.read(response.getContentAsString(), "$.errorCode");
  }

  private void expect(MockHttpServletResponse response, int status, String errorCode)
      throws Exception {
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
    assertThat(code(response)).isEqualTo(errorCode);
  }

  private void assertUntouched() {
    assertThat(count("SELECT count(*) FROM ai.prompt_templates")).isEqualTo(3);
    assertThat(count("SELECT count(*) FROM ai.prompt_templates WHERE status='ACTIVE'"))
        .isEqualTo(3);
  }

  // Read --------------------------------------------------------------------------------------

  @Test
  void listShowsThreeActiveRowsInFixedOrderAndTheDraftNextToItsActive() throws Exception {
    var first = call(get(BASE), admin);
    assertThat(first.getStatus()).isEqualTo(200);
    String body = first.getContentAsString();
    assertThat((List<?>) JsonPath.read(body, "$.items")).hasSize(3);
    assertThat((List<String>) JsonPath.read(body, "$.items[*].featureCode"))
        .containsExactly("SOLUTION_REVIEW", "MOCK_INTERVIEW", "GENERATE_TESTCASE");
    assertThat((List<String>) JsonPath.read(body, "$.items[*].status")).containsOnly("ACTIVE");
    assertThat((List<String>) JsonPath.read(body, "$.modelOptions"))
        .containsExactly("default", "model-a", "model-b");
    assertThat((Integer) JsonPath.read(body, "$.items[0].maxTokens")).isEqualTo(4096);
    assertThat((Object) JsonPath.read(body, "$.items[2].responseGuards")).isNull();

    assertThat(draft("SOLUTION_REVIEW", draftBody("d", "model-a", GUARDS), admin).getStatus())
        .isEqualTo(200);
    String after = call(get(BASE), admin).getContentAsString();
    assertThat((List<String>) JsonPath.read(after, "$.items[*].status"))
        .containsExactly("ACTIVE", "DRAFT", "ACTIVE", "ACTIVE");
    assertThat((List<String>) JsonPath.read(after, "$.items[*].featureCode"))
        .containsExactly(
            "SOLUTION_REVIEW", "SOLUTION_REVIEW", "MOCK_INTERVIEW", "GENERATE_TESTCASE");
  }

  // Draft -------------------------------------------------------------------------------------

  @Test
  void savingCreatesTheNextVersionDraftThenUpdatesTheSameRow() throws Exception {
    var created = draft("SOLUTION_REVIEW", draftBody("first", "model-a", GUARDS), admin);
    assertThat(created.getStatus()).as(created.getContentAsString()).isEqualTo(200);
    String body = created.getContentAsString();
    assertThat((String) JsonPath.read(body, "$.version")).isEqualTo("v1.1");
    assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("DRAFT");
    assertThat((Object) JsonPath.read(body, "$.publishedAt")).isNull();
    String id = JsonPath.read(body, "$.id");
    assertThat(activeId("SOLUTION_REVIEW")).isEqualTo(SEED[0]);

    String noSolution =
        "{\"citeLines\":false,\"answerInVietnamese\":true,\"noFullSolution\":false}";
    var updated = draft("SOLUTION_REVIEW", draftBody("second", "model-b", noSolution), admin);
    assertThat((String) JsonPath.read(updated.getContentAsString(), "$.id")).isEqualTo(id);
    assertThat(
            (Boolean)
                JsonPath.read(updated.getContentAsString(), "$.responseGuards.noFullSolution"))
        .isFalse();
    assertThat(count("SELECT count(*) FROM ai.prompt_templates WHERE status='DRAFT'")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT system_instruction FROM ai.prompt_templates WHERE id=?::uuid",
                String.class,
                id))
        .isEqualTo("second");
    assertThat(audits()).isZero();

    var testcase = draft("GENERATE_TESTCASE", draftBody("t", "model-a", "null"), admin);
    assertThat(testcase.getStatus()).isEqualTo(200);
    assertThat((Object) JsonPath.read(testcase.getContentAsString(), "$.responseGuards")).isNull();
  }

  @Test
  void theSeedAliasDefaultIsAcceptedOnDraftAndPublishWhileABogusIdStillIs422() throws Exception {
    // The test list is model-a,model-b: 'default' is absent from it and must still be valid.
    var saved = draft("SOLUTION_REVIEW", draftBody("uses the alias", "default", GUARDS), admin);
    assertThat(saved.getStatus()).as(saved.getContentAsString()).isEqualTo(200);
    assertThat((String) JsonPath.read(saved.getContentAsString(), "$.modelId"))
        .isEqualTo("default");
    var published = publish(publishBody(change("SOLUTION_REVIEW", SEED[0], "")), admin);
    assertThat(published.getStatus()).as(published.getContentAsString()).isEqualTo(200);
    assertThat((String) JsonPath.read(published.getContentAsString(), "$.items[0].modelId"))
        .isEqualTo("default");
    expect(draft("SOLUTION_REVIEW", draftBody("x", "bogus", GUARDS), admin), 422, "AIR-202");
    expect(
        publish(publishBody(change("MOCK_INTERVIEW", SEED[1], ",\"modelId\":\"bogus\"")), admin),
        422,
        "AIR-202");
  }

  @Test
  void draftValidationFollowsTheDdCodesAndLeavesNoRow() throws Exception {
    expect(draft("SOLUTION_REVIEW", draftBody("x", "evil", GUARDS), admin), 422, "AIR-202");
    expect(
        draft(
            "SOLUTION_REVIEW",
            "{\"systemInstruction\":\"x\",\"responseGuards\":" + GUARDS + "}",
            admin),
        422,
        "AIR-202");
    expect(draft("SOLUTION_REVIEW", draftBody("   ", "model-a", GUARDS), admin), 400, "AIR-V12");
    expect(
        draft("SOLUTION_REVIEW", draftBody("a".repeat(20_001), "model-a", GUARDS), admin),
        400,
        "AIR-V12");
    expect(draft("OTHER", draftBody("x", "model-a", GUARDS), admin), 400, "AIR-V11");
    expect(draft("GENERATE_TESTCASE", draftBody("x", "model-a", GUARDS), admin), 400, "AIR-V13");
    expect(draft("SOLUTION_REVIEW", draftBody("x", "model-a", "null"), admin), 400, "AIR-V13");
    expect(
        draft("SOLUTION_REVIEW", draftBody("x", "model-a", "{\"citeLines\":true}"), admin),
        400,
        "AIR-V13");
    expect(draft("SOLUTION_REVIEW", "not json", admin), 400, "AIR-V12");
    assertThat(count("SELECT count(*) FROM ai.prompt_templates")).isEqualTo(3);
    var edge = draft("SOLUTION_REVIEW", draftBody("a".repeat(20_000), "model-b", GUARDS), admin);
    assertThat(edge.getStatus()).isEqualTo(200);
  }

  // Publish -----------------------------------------------------------------------------------

  @Test
  void publishingADraftPromotesItArchivesTheOldRowAndWritesOneAuditRow() throws Exception {
    var saved = draft("SOLUTION_REVIEW", draftBody("secret prompt text", "model-b", GUARDS), admin);
    String draftId = JsonPath.read(saved.getContentAsString(), "$.id");
    var result = publish(publishBody(change("SOLUTION_REVIEW", SEED[0], "")), admin);
    assertThat(result.getStatus()).as(result.getContentAsString()).isEqualTo(200);
    String body = result.getContentAsString();
    assertThat((List<String>) JsonPath.read(body, "$.items[*].status")).containsOnly("ACTIVE");
    assertThat((List<?>) JsonPath.read(body, "$.items")).hasSize(3);
    assertThat((String) JsonPath.read(body, "$.items[0].id")).isEqualTo(draftId);
    assertThat((String) JsonPath.read(body, "$.items[0].version")).isEqualTo("v1.1");
    assertThat((String) JsonPath.read(body, "$.items[0].modelId")).isEqualTo("model-b");
    assertThat((Object) JsonPath.read(body, "$.items[0].publishedAt")).isNotNull();

    assertThat(count("SELECT count(*) FROM ai.prompt_templates WHERE status='DRAFT'")).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM ai.prompt_templates WHERE id=?::uuid", String.class, SEED[0]))
        .isEqualTo("ARCHIVED");
    assertThat(
            count(
                "SELECT count(*) FROM ai.prompt_templates WHERE feature_code='SOLUTION_REVIEW' AND status='ACTIVE'"))
        .isEqualTo(1);
    assertThat(audits()).isEqualTo(1);
    var row =
        jdbc.queryForMap(
            "SELECT actor_user_id::text AS actor, target_type, target_id, before_json::text AS b,"
                + " after_json::text AS a FROM identity.system_audit_logs"
                + " WHERE action_type='AI_PROMPT_PUBLISHED'");
    assertThat(row.get("actor")).isEqualTo(adminId.toString());
    assertThat(row.get("target_type")).isEqualTo("PROMPT_TEMPLATE");
    assertThat(row.get("target_id")).isEqualTo("SOLUTION_REVIEW");
    assertThat((String) row.get("b")).contains("v1.0").contains("default");
    assertThat((String) row.get("a")).contains("v1.1").contains("model-b");
    assertThat((String) row.get("a")).doesNotContain("secret prompt text");
  }

  @Test
  void publishingOnlyAModelCopiesTheInstructionAndBumpsTheVersion() throws Exception {
    String oldText =
        jdbc.queryForObject(
            "SELECT system_instruction FROM ai.prompt_templates WHERE id=?::uuid",
            String.class,
            SEED[1]);
    var result =
        publish(publishBody(change("MOCK_INTERVIEW", SEED[1], ",\"modelId\":\"model-a\"")), admin);
    assertThat(result.getStatus()).as(result.getContentAsString()).isEqualTo(200);
    var row =
        jdbc.queryForMap(
            "SELECT version, model_id, system_instruction FROM ai.prompt_templates"
                + " WHERE feature_code='MOCK_INTERVIEW' AND status='ACTIVE'");
    assertThat(row.get("version")).isEqualTo("v1.1");
    assertThat(row.get("model_id")).isEqualTo("model-a");
    assertThat(row.get("system_instruction")).isEqualTo(oldText);
    assertThat(audits()).isEqualTo(1);
  }

  @Test
  void aStaleActiveIdIsAConflictAndTheSecondAdminChangesNothing() throws Exception {
    String body = publishBody(change("SOLUTION_REVIEW", SEED[0], ",\"modelId\":\"model-a\""));
    assertThat(publish(body, admin).getStatus()).isEqualTo(200);
    expect(publish(body, admin), 409, "AIR-201");
    assertThat(count("SELECT count(*) FROM ai.prompt_templates")).isEqualTo(4);
    assertThat(audits()).isEqualTo(1);
  }

  @Test
  void twoAdminsPublishingAtTheSameTimeGetExactlyOneWinner() throws Exception {
    String body = publishBody(change("SOLUTION_REVIEW", SEED[0], ",\"modelId\":\"model-a\""));
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Integer> job = () -> publish(body, admin).getStatus();
      Future<Integer> a = pool.submit(job);
      Future<Integer> b = pool.submit(job);
      assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 409);
    } finally {
      pool.shutdownNow();
    }
    assertThat(
            count(
                "SELECT count(*) FROM ai.prompt_templates WHERE feature_code='SOLUTION_REVIEW' AND status='ACTIVE'"))
        .isEqualTo(1);
    assertThat(audits()).isEqualTo(1);
  }

  @Test
  void aBadSecondFeatureRollsBackTheFirstToo() throws Exception {
    String body =
        publishBody(
            change("SOLUTION_REVIEW", SEED[0], ",\"modelId\":\"model-a\""),
            change("MOCK_INTERVIEW", SEED[1], ",\"modelId\":\"evil\""));
    expect(publish(body, admin), 422, "AIR-202");
    assertUntouched();
    assertThat(audits()).isZero();
  }

  @Test
  void publishValidationAndNoChangeCodes() throws Exception {
    expect(publish("{\"changes\":[]}", admin), 400, "AIR-V14");
    expect(publish("{}", admin), 400, "AIR-V14");
    expect(
        publish(
            publishBody(
                change("SOLUTION_REVIEW", SEED[0], ""), change("SOLUTION_REVIEW", SEED[0], "")),
            admin),
        400,
        "AIR-V14");
    expect(publish(publishBody(change("OTHER", SEED[0], "")), admin), 400, "AIR-V11");
    expect(
        publish(
            publishBody(change("SOLUTION_REVIEW", SEED[0], ",\"systemInstruction\":\" \"")), admin),
        400,
        "AIR-V12");
    expect(
        publish(
            publishBody(change("GENERATE_TESTCASE", SEED[2], ",\"responseGuards\":" + GUARDS)),
            admin),
        400,
        "AIR-V13");
    expect(publish("garbage", admin), 400, "AIR-V14");
    String same =
        jdbc.queryForObject(
            "SELECT system_instruction FROM ai.prompt_templates WHERE id=?::uuid",
            String.class,
            SEED[0]);
    expect(
        publish(
            publishBody(
                change(
                    "SOLUTION_REVIEW",
                    SEED[0],
                    ",\"systemInstruction\":"
                        + JSON.writeValueAsString(same)
                        + ",\"responseGuards\":null")),
            admin),
        422,
        "AIR-205");
    assertUntouched();
  }

  @Test
  void historyListsEveryRowNewestFirstAndRejectsAnUnknownFeature() throws Exception {
    String model = ",\"modelId\":\"model-a\"";
    assertThat(publish(publishBody(change("SOLUTION_REVIEW", SEED[0], model)), admin).getStatus())
        .isEqualTo(200);
    assertThat(
            publish(
                    publishBody(
                        change(
                            "SOLUTION_REVIEW",
                            activeId("SOLUTION_REVIEW"),
                            ",\"modelId\":\"model-b\"")),
                    admin)
                .getStatus())
        .isEqualTo(200);
    draft("SOLUTION_REVIEW", draftBody("pending", "model-a", GUARDS), admin);
    var response = call(get(BASE + "/SOLUTION_REVIEW/history"), admin);
    assertThat(response.getStatus()).isEqualTo(200);
    String body = response.getContentAsString();
    assertThat((List<String>) JsonPath.read(body, "$.items[*].version"))
        .containsExactly("v1.3", "v1.2", "v1.1", "v1.0");
    assertThat((List<String>) JsonPath.read(body, "$.items[*].status"))
        .containsExactly("DRAFT", "ACTIVE", "ARCHIVED", "ARCHIVED");
    assertThat((String) JsonPath.read(body, "$.items[0].systemInstruction")).isEqualTo("pending");
    expect(call(get(BASE + "/OTHER/history"), admin), 400, "AIR-V11");
  }

  // Permissions -------------------------------------------------------------------------------

  private void allFourRefused(String bearer) throws Exception {
    expect(call(get(BASE), bearer), 403, "AIR-301");
    expect(call(get(BASE + "/SOLUTION_REVIEW/history"), bearer), 403, "AIR-301");
    expect(draft("SOLUTION_REVIEW", draftBody("x", "model-a", GUARDS), bearer), 403, "AIR-301");
    expect(
        publish(
            publishBody(change("SOLUTION_REVIEW", SEED[0], ",\"modelId\":\"model-a\"")), bearer),
        403,
        "AIR-301");
  }

  @Test
  void noTokenIs401() throws Exception {
    assertThat(call(get(BASE), null).getStatus()).isEqualTo(401);
    assertThat(publish("{}", null).getStatus()).isEqualTo(401);
  }

  @Test
  void readAndUpdateCellsAreSeparateAndBothNeedTheAdminCategory() throws Exception {
    grants("AI_CONFIG", "ADMIN", true, false);
    String readOnly = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    assertThat(call(get(BASE), readOnly).getStatus()).isEqualTo(200);
    assertThat(call(get(BASE + "/MOCK_INTERVIEW/history"), readOnly).getStatus()).isEqualTo(200);
    expect(draft("SOLUTION_REVIEW", draftBody("x", "model-a", GUARDS), readOnly), 403, "AIR-301");
    expect(
        publish(
            publishBody(change("SOLUTION_REVIEW", SEED[0], ",\"modelId\":\"model-a\"")), readOnly),
        403,
        "AIR-301");

    grants("AI_CONFIG", "ADMIN", false, true);
    String updateOnly = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    expect(call(get(BASE), updateOnly), 403, "AIR-301");
    assertThat(draft("SOLUTION_REVIEW", draftBody("x", "model-a", GUARDS), updateOnly).getStatus())
        .isEqualTo(200);

    grants("AI_CONFIG", "ADMIN", false, false);
    grants("AI_TOKEN_BUDGET", "ADMIN", true, true);
    allFourRefused(token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN));
  }

  @Test
  void aNonAdminWithBothCellsIsStillRefused() throws Exception {
    grants("AI_CONFIG", "STUDENT", true, true);
    allFourRefused(token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT));
    grants("AI_CONFIG", "STUDENT", false, false);
    allFourRefused(token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT));
    assertUntouched();
  }

  // Audit -------------------------------------------------------------------------------------

  @Test
  void anAuditFailureAnswers503AndRollsTheWholePublishBack() throws Exception {
    jdbc.execute(
        "CREATE FUNCTION identity.reject_ai_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
            + " IF NEW.action_type LIKE 'AI_%' THEN RAISE EXCEPTION 'test audit unavailable';"
            + " END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_ai_audit BEFORE INSERT ON identity.system_audit_logs FOR EACH ROW"
            + " EXECUTE FUNCTION identity.reject_ai_audit()");
    try {
      draft("SOLUTION_REVIEW", draftBody("pending", "model-a", GUARDS), admin);
      String body =
          publishBody(
              change("SOLUTION_REVIEW", SEED[0], ""),
              change("MOCK_INTERVIEW", SEED[1], ",\"modelId\":\"model-b\""));
      expect(publish(body, admin), 503, "AIR-503");
      assertThat(count("SELECT count(*) FROM ai.prompt_templates")).isEqualTo(4);
      assertThat(count("SELECT count(*) FROM ai.prompt_templates WHERE status='DRAFT'"))
          .isEqualTo(1);
      assertThat(activeId("SOLUTION_REVIEW")).isEqualTo(SEED[0]);
      assertThat(activeId("MOCK_INTERVIEW")).isEqualTo(SEED[1]);
    } finally {
      jdbc.execute("DROP TRIGGER reject_ai_audit ON identity.system_audit_logs");
      jdbc.execute("DROP FUNCTION identity.reject_ai_audit()");
    }
    assertThat(publish(publishBody(change("SOLUTION_REVIEW", SEED[0], "")), admin).getStatus())
        .isEqualTo(200);
  }

  // AC-04 -------------------------------------------------------------------------------------

  @Test
  void publishingANewVersionMakesTheCachedReviewMissForTheSameCode() throws Exception {
    UUID user = UUID.randomUUID();
    String student = token(user, "STUDENT", BaseCategory.STUDENT);
    UUID problem = UUID.randomUUID();
    UUID first = submission(user, problem, "print(1)");
    llm.ok(REVIEW);
    assertThat(review(first, student).getStatus()).isEqualTo(200);
    assertThat(llm.calls).hasSize(1);

    redis.delete("ai:cooldown:" + user);
    UUID sameCodeBefore = submission(user, problem, "print(1)");
    var hit = review(sameCodeBefore, student);
    assertThat((Boolean) JsonPath.read(hit.getContentAsString(), "$.servedFromCache")).isTrue();
    assertThat(llm.calls).hasSize(1);

    assertThat(
            publish(
                    publishBody(change("SOLUTION_REVIEW", SEED[0], ",\"modelId\":\"model-a\"")),
                    admin)
                .getStatus())
        .isEqualTo(200);

    redis.delete("ai:cooldown:" + user);
    UUID sameCodeAfter = submission(user, problem, "print(1)");
    llm.ok(REVIEW);
    var miss = review(sameCodeAfter, student);
    assertThat(miss.getStatus()).as(miss.getContentAsString()).isEqualTo(200);
    assertThat((Boolean) JsonPath.read(miss.getContentAsString(), "$.servedFromCache")).isFalse();
    assertThat(llm.calls).hasSize(2);
    assertThat(count("SELECT count(DISTINCT prompt_template_id) FROM ai.ai_cache_entries"))
        .isEqualTo(2);
  }

  private UUID submission(UUID owner, UUID problem, String code) {
    UUID id = UUID.randomUUID();
    submissions.accepted.put(id, new SubmissionSnapshot(id, owner, problem, "PYTHON", code));
    return id;
  }

  private MockHttpServletResponse review(UUID submission, String bearer) throws Exception {
    return call(post("/api/v1/ai/submissions/" + submission + "/solution-review"), bearer);
  }
}
