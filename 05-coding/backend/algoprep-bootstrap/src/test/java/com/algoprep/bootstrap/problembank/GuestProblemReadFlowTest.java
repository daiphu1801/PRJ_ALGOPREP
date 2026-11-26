package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guests on the published-problem reads, over HTTP through the real security chain (DD F2 section
 * 0A, AC-98 to AC-101): the four open routes answer without a session and carry no personal value,
 * a problem that is not published looks missing, every personal route still answers 401, and a
 * stale token reads the open routes as a guest but is still refused on the personal ones.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class GuestProblemReadFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/problems";
  private static final String BUCKET = "algoprep-testcases";

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

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
          .withEnv("MINIO_ROOT_USER", "test-guest-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-guest-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-guest-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-guest-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  @Autowired JwtEncoder encoder;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  private MockMvc mvc;
  private UUID me;
  private UUID sessionId;
  private String token;
  private String level;

  @BeforeEach
  void setup() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }
    jdbc.update("DELETE FROM problem.problem_tags");
    jdbc.update("DELETE FROM problem.problem_topics");
    jdbc.update("DELETE FROM problem.bookmarks");
    jdbc.update("DELETE FROM problem.testcases");
    jdbc.update("DELETE FROM problem.worked_examples");
    jdbc.update("DELETE FROM problem.problem_specs");
    jdbc.update("DELETE FROM problem.problem_stats");
    jdbc.update("DELETE FROM problem.problems");
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    me = UUID.randomUUID();
    sessionId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Reader',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        me,
        me + "@test.local");
    token = tokens.issue(me, BaseCategory.STUDENT, "STUDENT", sessionId).value();
    level =
        jdbc.queryForList(
                "SELECT id::text FROM problem.problem_levels ORDER BY sort_order", String.class)
            .get(0);
  }

  // AC-98
  @Test
  void aGuestReadsTheOpenRoutesAndGetsNoPersonalValue() throws Exception {
    var solved = problem("SOLVED1", "PUBLISHED", false);
    var saved = problem("SAVED1", "PUBLISHED", false);
    problem("DRAFT1", "UNPUBLISHED", false);
    problem("GONE1", "PUBLISHED", true);
    jdbc.update(
        "INSERT INTO identity.user_problem_best_score(user_id,problem_id,best_ratio,best_verdict) VALUES (?,?,1,'ACCEPTED')",
        me,
        solved);
    jdbc.update("INSERT INTO problem.bookmarks(user_id,problem_id) VALUES (?,?)", me, saved);
    testcase(saved, 1, "SAMPLE", true, "1 2", "3");
    testcase(saved, 2, "SAMPLE", true, "4 5", "9");
    testcase(saved, 3, "SAMPLE", false, "draft-in", "draft-out");
    testcase(saved, 4, "HIDDEN", true, "hidden-secret-input", "hidden-secret-output");

    var guestList = guest(BASE);
    assertThat(guestList.getStatus()).isEqualTo(200);
    var list = body(guestList);
    assertThat(list.path("solveStateAvailable").asBoolean()).isFalse();
    assertThat(list.path("items")).hasSize(2);
    list.path("items").forEach(item -> assertThat(item.path("solveState").isNull()).isTrue());

    // The status filter needs a person, so for a guest it is ignored, not an error.
    var filtered = guest(BASE + "?status=SOLVED");
    assertThat(filtered.getStatus()).isEqualTo(200);
    assertThat(body(filtered).path("items")).hasSize(2);

    var random = guest(BASE + "/random");
    assertThat(random.getStatus()).isEqualTo(200);
    assertThat(List.of(solved.toString(), saved.toString()))
        .contains(body(random).path("problemId").asString());

    // The level catalogue is open too; the problem counts stay an ADMIN-only extra.
    var levels = guest("/api/v1/problem-bank/levels?includeCounts=true");
    assertThat(levels.getStatus()).isEqualTo(200);
    assertThat(body(levels).path("items")).isNotEmpty();
    body(levels)
        .path("items")
        .forEach(
            item ->
                assertThat(item.has("problemCount") && !item.get("problemCount").isNull())
                    .isFalse());

    var overview = guest(BASE + "/" + saved + "/overview");
    assertThat(overview.getStatus()).isEqualTo(200);
    assertThat(body(overview).path("bookmarked").asBoolean()).isFalse();

    var samples = guest(BASE + "/" + saved + "/sample-testcases");
    assertThat(samples.getStatus()).isEqualTo(200);
    assertThat(body(samples).path("items")).hasSize(2);
    assertThat(samples.getContentAsString())
        .doesNotContain("hidden-secret", "draft-in", "draft-out");

    // Control: the same reads with a session still return the personal values.
    var signedInList = body(signedIn(BASE));
    assertThat(signedInList.path("solveStateAvailable").asBoolean()).isTrue();
    assertThat(solveStates(signedInList)).contains("SOLVED");
    assertThat(body(signedIn(BASE + "/" + saved + "/overview")).path("bookmarked").asBoolean())
        .isTrue();
  }

  // AC-99
  @Test
  void aProblemThatIsNotPublishedLooksMissingToAGuest() throws Exception {
    var draft = problem("DRAFT2", "UNPUBLISHED", false);
    var gone = problem("GONE2", "PUBLISHED", true);
    var bodies = new ArrayList<String>();
    for (String suffix : List.of("/overview", "/sample-testcases")) {
      for (String id :
          List.of(draft.toString(), gone.toString(), UUID.randomUUID().toString(), "nope")) {
        var response = guest(BASE + "/" + id + suffix);
        assertThat(response.getStatus()).as(id + suffix).isEqualTo(404);
        var json = body(response);
        assertThat(json.path("errorCode").asString()).isEqualTo("PRB-010");
        bodies.add(json.path("detail").asString());
      }
    }
    assertThat(bodies).containsOnly("Problem not found");
  }

  // AC-100
  @Test
  void everyPersonalRouteStillNeedsASession() throws Exception {
    var id = problem("P100", "PUBLISHED", false);
    for (String path :
        List.of(
            BASE + "/" + id + "/workspace",
            "/api/v1/problem-bank/topics/progress",
            "/api/v1/problem-bank/me/bookmarks",
            "/api/v1/problem-bank/suggested-problems")) {
      assertThat(guest(path).getStatus()).as(path).isEqualTo(401);
    }
    var write =
        mvc.perform(
                put("/api/v1/problem-bank/me/bookmarks/" + id + "/state")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"bookmarked\":true}"))
            .andReturn()
            .getResponse();
    assertThat(write.getStatus()).isEqualTo(401);
    var createLevel =
        mvc.perform(
                post("/api/v1/problem-bank/levels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"displayName\":\"Guest level\"}"))
            .andReturn()
            .getResponse();
    assertThat(createLevel.getStatus()).isEqualTo(401);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.bookmarks", Integer.class))
        .isZero();
  }

  // AC-101
  @Test
  void aStaleTokenReadsTheOpenRoutesAsAGuestButStaysRefusedOnPersonalOnes() throws Exception {
    var id = problem("P101", "PUBLISHED", false);
    jdbc.update(
        "INSERT INTO identity.user_problem_best_score(user_id,problem_id,best_ratio,best_verdict) VALUES (?,?,1,'ACCEPTED')",
        me,
        id);
    var revokedSession = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.session_revocations(family_id,revoked_at) VALUES (?, now())",
        revokedSession);
    var stale =
        List.of(
            expiredToken(),
            token.substring(0, token.length() - 4) + "AAAA",
            tokens.issue(me, BaseCategory.STUDENT, "STUDENT", revokedSession).value());

    for (String bad : stale) {
      var list = withToken(BASE, bad);
      assertThat(list.getStatus()).isEqualTo(200);
      assertThat(body(list).path("solveStateAvailable").asBoolean()).isFalse();
      assertThat(withToken(BASE + "/" + id + "/overview", bad).getStatus()).isEqualTo(200);
      assertThat(withToken(BASE + "/" + id + "/workspace", bad).getStatus()).isEqualTo(401);
    }
    // A good token on the same open route keeps the personal values (checked once, not dropped).
    assertThat(body(signedIn(BASE)).path("solveStateAvailable").asBoolean()).isTrue();
  }

  // ---- helpers

  private String expiredToken() {
    var claims =
        JwtClaimsSet.builder()
            .subject(me.toString())
            .claim("base_category", "STUDENT")
            .claim("sid", sessionId.toString())
            .issuedAt(Instant.now().minusSeconds(7200))
            .expiresAt(Instant.now().minusSeconds(3600))
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
        .getTokenValue();
  }

  private MockHttpServletResponse guest(String path) throws Exception {
    return mvc.perform(get(path)).andReturn().getResponse();
  }

  private MockHttpServletResponse signedIn(String path) throws Exception {
    return withToken(path, token);
  }

  private MockHttpServletResponse withToken(String path, String bearer) throws Exception {
    MockHttpServletRequestBuilder request = get(path).header("Authorization", "Bearer " + bearer);
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode body(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private static List<String> solveStates(JsonNode list) {
    var states = new ArrayList<String>();
    list.path("items").forEach(i -> states.add(i.path("solveState").asString()));
    return states;
  }

  private UUID problem(String code, String status, boolean deleted) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,deleted,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,?,'statement',?,?::uuid,?,?,1000,256,64)",
        id,
        code,
        "Title " + code,
        UUID.randomUUID(),
        level,
        deleted ? "UNPUBLISHED" : status,
        deleted);
    if ("PUBLISHED".equals(status) && !deleted) {
      jdbc.update(
          "INSERT INTO problem.problem_specs(id,problem_id,stdin_format_md,stdout_format_md,function_name,return_type,parameters) VALUES (?,?,'in','out','solve','{\"kind\":\"INT\"}'::jsonb,'[{\"name\":\"nums\",\"type\":{\"kind\":\"INT\"}}]'::jsonb)",
          UUID.randomUUID(),
          id);
    }
    return id;
  }

  private void testcase(
      UUID problem, int position, String visibility, boolean approved, String... inAndOut) {
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,expected_output_storage,expected_output_inline,origin,approved,position) VALUES (?,?,?,'INLINE',?,'INLINE',?,'MANUAL',?,?)",
        UUID.randomUUID(),
        problem,
        visibility,
        inAndOut[0],
        inAndOut[1],
        approved,
        position);
  }
}
