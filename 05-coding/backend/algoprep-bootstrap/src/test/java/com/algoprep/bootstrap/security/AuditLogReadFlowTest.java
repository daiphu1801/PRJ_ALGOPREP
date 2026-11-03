package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * #39, #40, #64, #65, #66 and BR-16 against real PostgreSQL and Redis over real HTTP (a real port,
 * because CSV and SSE are streamed). Audit rows are seeded by SQL; the admin comes through {@code
 * /auth/login}. The stream runs with small intervals so the test stays deterministic and fast.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=8",
      "algoprep.identity.audit-stream.poll-interval=200ms",
      "algoprep.identity.audit-stream.heartbeat-interval=300ms",
      "algoprep.identity.audit-stream.max-subscribers=3"
    })
@ActiveProfiles("test")
@Testcontainers
class AuditLogReadFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final ZoneOffset LOCAL = ZoneOffset.ofHours(7);
  private static final String BASE_PATH = "/api/v1/identity/admin/audit-logs";

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
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @LocalServerPort private int port;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AccessTokenPort tokens;

  @Autowired
  @Qualifier("auditStreamSubscribers")
  private IntSupplier subscribers;

  private final HttpClient http = HttpClient.newHttpClient();
  private final List<Sse> streams = new ArrayList<>();
  private UUID admin;
  private String adminToken;

  @BeforeEach
  void setup() throws Exception {
    jdbc.update("DELETE FROM identity.system_audit_logs");
    String email = UUID.randomUUID() + "@admin.test";
    admin = user("ADMIN", email, "Admin Main");
    adminToken = login(email);
  }

  @AfterEach
  void closeStreams() {
    streams.forEach(Sse::close);
    streams.clear();
    assertThat(await(() -> subscribers.getAsInt() == 0, 20)).as("viewers cleaned up").isTrue();
  }

  // ---- #39 pagination ------------------------------------------------------------------------

  @Test
  void anEmptyLogGivesAnEmptyPageAndNoCursor() throws Exception {
    HttpResponse<String> response = get(BASE_PATH, adminToken);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(JsonPath.<List<?>>read(response.body(), "$.items")).isEmpty();
    assertThat(JsonPath.<Object>read(response.body(), "$.nextCursor")).isNull();
  }

  @Test
  void exactlyFiftyRowsIsOnePageWithoutACursor() throws Exception {
    bulk(admin, 50);

    HttpResponse<String> response = get(BASE_PATH, adminToken);

    assertThat(JsonPath.<Integer>read(response.body(), "$.items.length()")).isEqualTo(50);
    assertThat(JsonPath.<Object>read(response.body(), "$.nextCursor")).isNull();
  }

  @Test
  void fiftyOneRowsPageAtFiftyAndTheTieAtTheBoundaryLosesNothing() throws Exception {
    Instant base = Instant.now().minusSeconds(3600);
    for (int i = 1; i <= 49; i++) {
      audit(admin, "PASSWORD_CHANGED", null, base.minusSeconds(i));
    }
    UUID low = tie("00000000-0000-0000-0000-0000000000a1", base.minusSeconds(50));
    UUID high = tie("00000000-0000-0000-0000-0000000000a2", base.minusSeconds(50));

    HttpResponse<String> first = get(BASE_PATH, adminToken);
    String cursor = JsonPath.read(first.body(), "$.nextCursor");
    HttpResponse<String> second = get(BASE_PATH + "?cursor=" + enc(cursor), adminToken);

    assertThat(JsonPath.<Integer>read(first.body(), "$.items.length()")).isEqualTo(50);
    assertThat(JsonPath.<String>read(first.body(), "$.items[49].id")).isEqualTo(high.toString());
    assertThat(JsonPath.<Integer>read(second.body(), "$.items.length()")).isEqualTo(1);
    assertThat(JsonPath.<String>read(second.body(), "$.items[0].id")).isEqualTo(low.toString());
    assertThat(JsonPath.<Object>read(second.body(), "$.nextCursor")).isNull();
    assertThat(cursor).doesNotContain(high.toString());
  }

  @Test
  void itemsAreNewestFirstAndCarryTheDtoFieldsAndNoRawSnapshots() throws Exception {
    Instant now = Instant.now();
    audit(admin, "PASSWORD_CHANGED", null, now.minusSeconds(20));
    UUID newest = audit(admin, "ROLE_DELETED", "{\"name\":\"Role X\"}", now.minusSeconds(10));

    HttpResponse<String> response = get(BASE_PATH, adminToken);

    assertThat(JsonPath.<String>read(response.body(), "$.items[0].id"))
        .isEqualTo(newest.toString());
    assertThat(JsonPath.<String>read(response.body(), "$.items[0].actionType"))
        .isEqualTo("ROLE_DELETED");
    assertThat(JsonPath.<String>read(response.body(), "$.items[0].categoryCode"))
        .isEqualTo("PERMISSION");
    assertThat(JsonPath.<String>read(response.body(), "$.items[0].serviceCode"))
        .isEqualTo("permission");
    assertThat(JsonPath.<String>read(response.body(), "$.items[0].actorDisplayName"))
        .isEqualTo("Admin Main");
    assertThat(JsonPath.<String>read(response.body(), "$.items[0].safeParams.name"))
        .isEqualTo("Role X");
    assertThat(response.body())
        .doesNotContain("before_json", "after_json", "beforeJson", "afterJson");
  }

  // ---- #39 filters ---------------------------------------------------------------------------

  @Test
  void keywordMatchesActorEmailEventCodeAndServiceName() throws Exception {
    UUID other = user("ADMIN", "zed.unique@admin.test", "Zed");
    UUID byOther = audit(other, "ROLE_DELETED", null, Instant.now().minusSeconds(5));
    audit(admin, "PASSWORD_CHANGED", null, Instant.now().minusSeconds(6));

    assertThat(ids(get(BASE_PATH + "?q=ZED.unique", adminToken)))
        .containsExactly(byOther.toString());
    String code = "evt_" + byOther.toString().substring(0, 8);
    assertThat(ids(get(BASE_PATH + "?q=" + code, adminToken))).containsExactly(byOther.toString());
    assertThat(ids(get(BASE_PATH + "?q=permission", adminToken)))
        .containsExactly(byOther.toString());
    assertThat(ids(get(BASE_PATH + "?q=no-such-thing", adminToken))).isEmpty();
  }

  @Test
  void keywordOfExactlyOneHundredCharactersIsAccepted() throws Exception {
    assertThat(get(BASE_PATH + "?q=" + "a".repeat(100), adminToken).statusCode()).isEqualTo(200);
    assertThat(get(BASE_PATH + "?q=%20%20" + "a".repeat(100) + "%20", adminToken).statusCode())
        .isEqualTo(200);
  }

  @Test
  void categoryFilterSelectsTheGroupAndConfigAlsoTakesUnmappedActions() throws Exception {
    Instant now = Instant.now();
    UUID auth = audit(admin, "PASSWORD_CHANGED", null, now.minusSeconds(1));
    UUID perm = audit(admin, "USER_LOCKED", null, now.minusSeconds(2));
    UUID conf = audit(admin, "QUEUE_PAUSE_CHANGED", null, now.minusSeconds(3));
    UUID unmapped = audit(admin, "BRAND_NEW_ACTION", null, now.minusSeconds(4));
    UUID content = audit(admin, "PROBLEM_PUBLISHED", null, now.minusSeconds(5));

    assertThat(ids(get(BASE_PATH + "?categoryCode=AUTH", adminToken)))
        .containsExactly(auth.toString());
    assertThat(ids(get(BASE_PATH + "?categoryCode=PERMISSION", adminToken)))
        .containsExactly(perm.toString());
    assertThat(ids(get(BASE_PATH + "?categoryCode=CONFIG", adminToken)))
        .containsExactly(conf.toString(), unmapped.toString());
    assertThat(ids(get(BASE_PATH + "?categoryCode=CONTENT", adminToken)))
        .containsExactly(content.toString());
    assertThat(ids(get(BASE_PATH + "?categoryCode=ALL", adminToken))).hasSize(5);
  }

  @Test
  void dateRangeIsClosedAtBothEndsInLocalTime() throws Exception {
    LocalDate today = LocalDate.now(LOCAL);
    LocalDate day = today.minusDays(1);
    UUID startOfDay = audit(admin, "PASSWORD_CHANGED", null, day.atStartOfDay(LOCAL).toInstant());
    UUID endOfDay =
        audit(
            admin,
            "PASSWORD_CHANGED",
            null,
            day.plusDays(1).atStartOfDay(LOCAL).toInstant().minusMillis(1));
    audit(admin, "PASSWORD_CHANGED", null, day.plusDays(1).atStartOfDay(LOCAL).toInstant());
    audit(admin, "PASSWORD_CHANGED", null, day.atStartOfDay(LOCAL).toInstant().minusMillis(1));

    HttpResponse<String> one = get(BASE_PATH + "?dateFrom=" + day + "&dateTo=" + day, adminToken);

    assertThat(ids(one)).containsExactlyInAnyOrder(startOfDay.toString(), endOfDay.toString());
    assertThat(ids(get(BASE_PATH + "?dateFrom=" + today, adminToken))).hasSize(1);
    assertThat(ids(get(BASE_PATH + "?dateTo=" + day, adminToken))).hasSize(3);
  }

  @Test
  void filtersAndCursorTogetherKeepPagingInsideTheFilter() throws Exception {
    Instant base = Instant.now().minusSeconds(600);
    for (int i = 0; i < 120; i++) {
      audit(admin, i % 2 == 0 ? "PASSWORD_CHANGED" : "ROLE_DELETED", null, base.minusSeconds(i));
    }

    HttpResponse<String> first = get(BASE_PATH + "?categoryCode=AUTH", adminToken);
    String cursor = JsonPath.read(first.body(), "$.nextCursor");
    HttpResponse<String> second =
        get(BASE_PATH + "?categoryCode=AUTH&cursor=" + enc(cursor), adminToken);

    assertThat(JsonPath.<Integer>read(first.body(), "$.items.length()")).isEqualTo(50);
    assertThat(JsonPath.<Integer>read(second.body(), "$.items.length()")).isEqualTo(10);
    assertThat(JsonPath.<Object>read(second.body(), "$.nextCursor")).isNull();
    assertThat(JsonPath.<List<String>>read(second.body(), "$.items[*].actionType"))
        .containsOnly("PASSWORD_CHANGED");
  }

  // ---- #39 / #40 errors ----------------------------------------------------------------------

  @Test
  void aKeywordOverOneHundredCharactersIsIdt444OnSearchAndExport() throws Exception {
    String longQ = "a".repeat(101);

    assertError(get(BASE_PATH + "?q=" + longQ, adminToken), 400, "IDT-444");
    assertError(get(BASE_PATH + "/export?columns=id&q=" + longQ, adminToken), 400, "IDT-444");
  }

  @Test
  void badCategoryDatesAndCursorsAreIdt443() throws Exception {
    LocalDate today = LocalDate.now(LOCAL);
    String[] bad = {
      "categoryCode=auth",
      "categoryCode=USER",
      "dateFrom=2026-13-40",
      "dateFrom=01/11/2026",
      "dateTo=" + today.plusDays(1),
      "dateFrom=" + today.plusDays(1),
      "dateFrom=" + today + "&dateTo=" + today.minusDays(1),
      "dateFrom=" + today.minusDays(91),
      "cursor=not-a-cursor"
    };
    for (String query : bad) {
      assertError(get(BASE_PATH + "?" + query, adminToken), 400, "IDT-443");
    }
    assertThat(get(BASE_PATH + "?dateFrom=" + today.minusDays(90), adminToken).statusCode())
        .isEqualTo(200);
  }

  @Test
  void aCursorIssuedForOneFilterIsRejectedUnderAnother() throws Exception {
    bulk(admin, 60);
    String cursor = JsonPath.read(get(BASE_PATH, adminToken).body(), "$.nextCursor");

    assertError(
        get(BASE_PATH + "?categoryCode=AUTH&cursor=" + enc(cursor), adminToken), 400, "IDT-443");
  }

  // ---- BR-16 masking -------------------------------------------------------------------------

  @Test
  void sensitiveSnapshotValuesAreMaskedInSearchAndExportAndNeverLeak() throws Exception {
    String after =
        "{\"password_hash\":\"HASHVALUE\",\"token_hash\":\"TOKVALUE\",\"nested\":{\"apiSecret\":"
            + "\"SECRETVALUE\"},\"keep\":\"visible\"}";
    audit(admin, "USER_CREATED", after, Instant.now().minusSeconds(5));

    HttpResponse<String> search = get(BASE_PATH, adminToken);
    HttpResponse<String> csv = get(BASE_PATH + "/export?columns=content", adminToken);

    assertThat(JsonPath.<String>read(search.body(), "$.items[0].safeParams.password_hash"))
        .isEqualTo("***");
    assertThat(JsonPath.<String>read(search.body(), "$.items[0].safeParams.token_hash"))
        .isEqualTo("***");
    assertThat(JsonPath.<String>read(search.body(), "$.items[0].safeParams.nested.apiSecret"))
        .isEqualTo("***");
    assertThat(JsonPath.<String>read(search.body(), "$.items[0].safeParams.keep"))
        .isEqualTo("visible");
    assertThat(search.body() + csv.body()).doesNotContain("HASHVALUE", "TOKVALUE", "SECRETVALUE");
    assertThat(csv.body()).contains("password_hash=***", "keep=visible");
  }

  // ---- #40 CSV -------------------------------------------------------------------------------

  @Test
  void csvHasBomHeaderSelectedColumnsQuotingAndFormulaGuard() throws Exception {
    UUID evil = user("ADMIN", "=cmd@admin.test", "Evil");
    String after = "{\"note\":\"a,b \\\"q\\\"\\nline2\"}";
    UUID id = audit(evil, "BRAND_NEW_ACTION", after, Instant.now().minusSeconds(5));

    HttpResponse<String> response =
        get(BASE_PATH + "/export?columns=actor,id,content&format=csv", adminToken);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .startsWith("text/csv")
        .containsIgnoringCase("utf-8");
    assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    String body = response.body();
    assertThat(body).startsWith("\uFEFF" + "Người thực hiện,Mã sự kiện,Nội dung\r\n");
    assertThat(body).contains("'=cmd@admin.test," + id + ",");
    assertThat(body).contains("\"Hành động BRAND_NEW_ACTION trên USER ");
    assertThat(body).contains("note=a,b \"\"q\"\" line2");
    assertThat(body).doesNotContain("Thời gian", "Dịch vụ", "Phân loại");
  }

  @Test
  void csvHonoursAcceptLanguageAndTheFilters() throws Exception {
    Instant now = Instant.now();
    audit(admin, "PASSWORD_CHANGED", null, now.minusSeconds(3));
    audit(admin, "ROLE_DELETED", null, now.minusSeconds(4));

    HttpResponse<String> response =
        getWith(
            BASE_PATH + "/export?columns=time,category,content&categoryCode=AUTH",
            adminToken,
            "en-US,en;q=0.9");

    String[] lines = response.body().split("\r\n");
    assertThat(lines[0]).isEqualTo("\uFEFFTime,Category,Content");
    assertThat(lines).hasSize(2);
    assertThat(lines[1])
        .contains("AUTH,User changed the password")
        .matches("^20\\d\\d-.*\\+07:00,.*");
  }

  @Test
  void csvStreamsMoreThanOneInternalPageInOrder() throws Exception {
    bulk(admin, 1203);

    HttpResponse<String> response = get(BASE_PATH + "/export?columns=id,time", adminToken);

    String[] lines = response.body().split("\r\n");
    // Header + 1203 seeded rows + the export's own audit row, which is written before the body.
    assertThat(lines).hasSize(1205);
    List<java.time.OffsetDateTime> times = new ArrayList<>();
    for (int i = 1; i < lines.length; i++) {
      times.add(java.time.OffsetDateTime.parse(lines[i].substring(lines[i].indexOf(',') + 1)));
    }
    assertThat(times).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(times).doesNotHaveDuplicates();
    assertThat(lines[1])
        .contains(
            jdbc.queryForObject(
                "SELECT id::text FROM identity.system_audit_logs WHERE action_type = 'AUDIT_LOG_EXPORTED'",
                String.class));
  }

  @Test
  void anExportIsRecordedOnceAndAFailedValidationRecordsNothing() throws Exception {
    assertError(get(BASE_PATH + "/export?columns=id,nope", adminToken), 400, "IDT-443");
    assertError(get(BASE_PATH + "/export?columns=id,id", adminToken), 400, "IDT-443");
    assertError(get(BASE_PATH + "/export", adminToken), 400, "IDT-443");
    assertError(get(BASE_PATH + "/export?columns=id&format=xlsx", adminToken), 400, "IDT-443");
    assertError(
        get(BASE_PATH + "/export?columns=id&dateTo=2999-01-01", adminToken), 400, "IDT-443");
    assertThat(count("AUDIT_LOG_EXPORTED")).isZero();

    assertThat(get(BASE_PATH + "/export?columns=id&categoryCode=AUTH", adminToken).statusCode())
        .isEqualTo(200);

    assertThat(count("AUDIT_LOG_EXPORTED")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT actor_user_id FROM identity.system_audit_logs WHERE action_type ="
                    + " 'AUDIT_LOG_EXPORTED'",
                UUID.class))
        .isEqualTo(admin);
    assertThat(
            jdbc.queryForObject(
                "SELECT after_json::text FROM identity.system_audit_logs WHERE action_type ="
                    + " 'AUDIT_LOG_EXPORTED'",
                String.class))
        .contains("\"columns\"")
        .contains("AUTH");
  }

  // ---- #64, #65 ------------------------------------------------------------------------------

  @Test
  void activeAdminsAreTheFiveBusiestAdminsOfTheLastDayOnly() throws Exception {
    List<UUID> admins = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      admins.add(user("ADMIN", "busy" + i + "@admin.test", "Busy " + i));
    }
    for (int i = 0; i < 6; i++) {
      for (int n = 0; n <= i; n++) {
        audit(admins.get(i), "ROLE_DELETED", null, Instant.now().minusSeconds(60L + n));
      }
    }
    audit(admins.get(0), "ROLE_DELETED", null, Instant.now().minus(Duration.ofHours(30)));
    UUID student = user("STUDENT", "student.actor@example.test", "Student");
    audit(student, "PASSWORD_CHANGED", null, Instant.now().minusSeconds(5));

    HttpResponse<String> response = get(BASE_PATH + "/active-admins", adminToken);

    assertThat(JsonPath.<Integer>read(response.body(), "$.items.length()")).isEqualTo(5);
    assertThat(JsonPath.<List<String>>read(response.body(), "$.items[*].actorUserId"))
        .containsExactly(
            admins.get(5).toString(),
            admins.get(4).toString(),
            admins.get(3).toString(),
            admins.get(2).toString(),
            admins.get(1).toString());
    assertThat(JsonPath.<Integer>read(response.body(), "$.items[0].actionCount")).isEqualTo(6);
    assertThat(JsonPath.<String>read(response.body(), "$.items[0].displayName"))
        .isEqualTo("Busy 5");
    assertThat(JsonPath.<String>read(response.body(), "$.items[0].roleName")).isNotBlank();
    assertThat(JsonPath.<String>read(response.body(), "$.items[0].lastActionAt")).isNotBlank();
  }

  @Test
  void activeAdminsOfAQuietDayIsAnEmptyList() throws Exception {
    audit(admin, "ROLE_DELETED", null, Instant.now().minus(Duration.ofHours(25)));

    assertThat(
            JsonPath.<List<?>>read(get(BASE_PATH + "/active-admins", adminToken).body(), "$.items"))
        .isEmpty();
  }

  @Test
  void categoryBreakdownCountsSevenDaysInFourGroupsEvenWhenZero() throws Exception {
    HttpResponse<String> empty = get(BASE_PATH + "/category-breakdown", adminToken);
    assertThat(JsonPath.<List<String>>read(empty.body(), "$.items[*].categoryCode"))
        .containsExactly("AUTH", "PERMISSION", "CONFIG", "CONTENT");
    assertThat(JsonPath.<List<Integer>>read(empty.body(), "$.items[*].count"))
        .containsExactly(0, 0, 0, 0);

    Instant now = Instant.now();
    audit(admin, "PASSWORD_CHANGED", null, now.minusSeconds(5));
    audit(admin, "EMAIL_CHANGED", null, now.minusSeconds(6));
    audit(admin, "ROLE_DELETED", null, now.minus(Duration.ofDays(6)));
    audit(admin, "BRAND_NEW_ACTION", null, now.minus(Duration.ofDays(2)));
    audit(admin, "PROBLEM_PUBLISHED", null, now.minus(Duration.ofDays(8)));

    HttpResponse<String> filled = get(BASE_PATH + "/category-breakdown", adminToken);

    assertThat(JsonPath.<List<Integer>>read(filled.body(), "$.items[*].count"))
        .containsExactly(2, 1, 1, 0);
  }

  // ---- access control ------------------------------------------------------------------------

  @Test
  void studentAndInstructorGet403Idt447OnEveryEndpointAndNoTokenGets401() throws Exception {
    String student = tokenFor("STUDENT", BaseCategory.STUDENT);
    String instructor = tokenFor("INSTRUCTOR", BaseCategory.INSTRUCTOR);
    String[] paths = {
      BASE_PATH,
      BASE_PATH + "/export?columns=id",
      BASE_PATH + "/active-admins",
      BASE_PATH + "/category-breakdown",
      BASE_PATH + "/stream"
    };
    for (String path : paths) {
      assertError(get(path, student), 403, "IDT-447");
      assertError(get(path, instructor), 403, "IDT-447");
      assertThat(get(path, null).statusCode()).isEqualTo(401);
    }
    assertThat(count("AUDIT_LOG_EXPORTED")).as("a refused export is not recorded").isZero();
    assertThat(subscribers.getAsInt()).isZero();
  }

  // ---- #66 SSE -------------------------------------------------------------------------------

  @Test
  void aNewRowIsPushedMaskedAndAHeartbeatKeepsTheStreamAlive() throws Exception {
    Sse sse = open(adminToken, null);
    assertThat(sse.status()).isEqualTo(200);
    assertThat(sse.cacheControl()).contains("no-store").contains("no-transform");
    assertThat(await(() -> subscribers.getAsInt() == 1, 10)).isTrue();

    UUID id = audit(admin, "USER_CREATED", "{\"token\":\"LEAKME\",\"name\":\"N\"}", Instant.now());

    // Spring writes the fields in builder order: id, event, data.
    assertThat(sse.awaitLine(l -> l.startsWith("id:"), 10)).isEqualTo("id:" + id);
    assertThat(sse.awaitLine(l -> l.startsWith("event:"), 10)).isEqualTo("event:audit-log");
    String data = sse.awaitLine(l -> l.startsWith("data:"), 10).substring("data:".length());
    assertThat(JsonPath.<String>read(data, "$.id")).isEqualTo(id.toString());
    assertThat(JsonPath.<String>read(data, "$.actionType")).isEqualTo("USER_CREATED");
    assertThat(JsonPath.<String>read(data, "$.categoryCode")).isEqualTo("PERMISSION");
    assertThat(JsonPath.<String>read(data, "$.safeParams.token")).isEqualTo("***");
    assertThat(data).doesNotContain("LEAKME");
    assertThat(sse.awaitLine(l -> l.contains("heartbeat"), 10)).startsWith(":");
  }

  @Test
  void aClosedClientIsRemovedAndThePollerStopsWithTheLastViewer() throws Exception {
    Sse sse = open(adminToken, null);
    assertThat(await(() -> subscribers.getAsInt() == 1, 10)).isTrue();

    sse.close();

    assertThat(await(() -> subscribers.getAsInt() == 0, 20)).as("viewer removed").isTrue();
    Sse again = open(adminToken, null);
    assertThat(await(() -> subscribers.getAsInt() == 1, 10)).isTrue();
    UUID id = audit(admin, "PASSWORD_CHANGED", null, Instant.now());
    assertThat(again.awaitLine(l -> l.startsWith("id:"), 10)).isEqualTo("id:" + id);
  }

  @Test
  void reconnectingWithLastEventIdReplaysTheMissedRowsOnceAndThenGoesLive() throws Exception {
    Instant now = Instant.now();
    UUID seen = audit(admin, "PASSWORD_CHANGED", null, now.minusSeconds(30));
    UUID missed1 = audit(admin, "EMAIL_CHANGED", null, now.minusSeconds(20));
    UUID missed2 = audit(admin, "ROLE_DELETED", null, now.minusSeconds(10));

    Sse sse = open(adminToken, seen.toString());
    assertThat(sse.awaitLine(l -> l.startsWith("id:"), 10)).isEqualTo("id:" + missed1);
    assertThat(sse.awaitLine(l -> l.startsWith("id:"), 10)).isEqualTo("id:" + missed2);
    UUID live = audit(admin, "USER_LOCKED", null, Instant.now());

    assertThat(sse.awaitLine(l -> l.startsWith("id:"), 10)).isEqualTo("id:" + live);
  }

  @Test
  void anUnknownMalformedOrExpiredLastEventIdMeansResync() throws Exception {
    UUID expired =
        audit(admin, "PASSWORD_CHANGED", null, Instant.now().minus(Duration.ofDays(100)));
    for (String marker :
        new String[] {UUID.randomUUID().toString(), "not-a-uuid", expired.toString()}) {
      Sse sse = open(adminToken, marker);
      assertThat(sse.awaitLine(l -> l.startsWith("event:"), 10)).isEqualTo("event:resync");
      sse.close();
      assertThat(await(() -> subscribers.getAsInt() == 0, 20)).isTrue();
    }
  }

  @Test
  void viewersBeyondTheCeilingGet429Idt449() throws Exception {
    for (int i = 0; i < 3; i++) {
      assertThat(open(adminToken, null).status()).isEqualTo(200);
    }
    assertThat(await(() -> subscribers.getAsInt() == 3, 10)).isTrue();

    assertError(get(BASE_PATH + "/stream", adminToken), 429, "IDT-449");
    assertThat(subscribers.getAsInt()).isEqualTo(3);
  }

  // ---- helpers -------------------------------------------------------------------------------

  private UUID user(String roleCode, String email, String name) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users (id, email, password_hash, display_name, role_id, status,"
            + " must_change_password) SELECT ?, ?, ?, ?, r.id, 'ACTIVE', false FROM"
            + " identity.roles r WHERE r.code = ?",
        id,
        email,
        HASH,
        name,
        roleCode);
    return id;
  }

  private String tokenFor(String roleCode, BaseCategory category) {
    UUID id = user(roleCode, UUID.randomUUID() + "@role.test", "User " + roleCode);
    return tokens.issue(id, category, roleCode, UUID.randomUUID()).value();
  }

  private String login(String email) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(base() + "/api/v1/identity/auth/login"))
            .header("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
            .build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return JsonPath.read(response.body(), "$.accessToken");
  }

  private UUID audit(UUID actor, String action, String afterJson, Instant at) {
    UUID id = UUID.randomUUID();
    insertAudit(id, actor, action, afterJson, at);
    return id;
  }

  private UUID tie(String id, Instant at) {
    UUID fixed = UUID.fromString(id);
    insertAudit(fixed, admin, "PASSWORD_CHANGED", null, at);
    return fixed;
  }

  private void insertAudit(UUID id, UUID actor, String action, String afterJson, Instant at) {
    jdbc.update(
        "INSERT INTO identity.system_audit_logs (id, actor_user_id, action_type, target_type,"
            + " target_id, after_json, created_at) VALUES (?, ?, ?, 'USER', ?, CAST(? AS jsonb), ?)",
        id,
        actor,
        action,
        "target-" + id.toString().substring(0, 8),
        afterJson,
        Timestamp.from(at));
  }

  private void bulk(UUID actor, int rows) {
    jdbc.update(
        "INSERT INTO identity.system_audit_logs (id, actor_user_id, action_type, target_type,"
            + " target_id, created_at) SELECT gen_random_uuid(), ?, 'PASSWORD_CHANGED', 'USER',"
            + " 'bulk', now() - make_interval(secs => g) FROM generate_series(1, ?) g",
        actor,
        rows);
  }

  private int count(String action) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE action_type = ?",
        Integer.class,
        action);
  }

  private String base() {
    return "http://localhost:" + port;
  }

  private static String enc(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private HttpResponse<String> get(String path, String token) throws Exception {
    return getWith(path, token, null);
  }

  private HttpResponse<String> getWith(String path, String token, String language)
      throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base() + path));
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    if (language != null) {
      request.header("Accept-Language", language);
    }
    return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private static List<String> ids(HttpResponse<String> response) {
    assertThat(response.statusCode()).isEqualTo(200);
    return JsonPath.read(response.body(), "$.items[*].id");
  }

  private static void assertError(HttpResponse<String> response, int status, String code) {
    assertThat(response.statusCode()).isEqualTo(status);
    assertThat(JsonPath.<String>read(response.body(), "$.errorCode")).isEqualTo(code);
  }

  private static boolean await(BooleanSupplier condition, int seconds) {
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
    while (System.nanoTime() < end) {
      if (condition.getAsBoolean()) {
        return true;
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return condition.getAsBoolean();
  }

  private Sse open(String token, String lastEventId) {
    Sse sse = new Sse(base() + BASE_PATH + "/stream", token, lastEventId);
    streams.add(sse);
    return sse;
  }

  /** One open event stream read line by line on its own client, so closing it drops the socket. */
  private static final class Sse {
    private final HttpClient client = HttpClient.newHttpClient();
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();
    private final java.util.concurrent.CompletableFuture<HttpResponse<Stream<String>>> response;

    Sse(String url, String token, String lastEventId) {
      HttpRequest.Builder request =
          HttpRequest.newBuilder(URI.create(url))
              .header("Authorization", "Bearer " + token)
              .header("Accept", "text/event-stream");
      if (lastEventId != null) {
        request.header("Last-Event-ID", lastEventId);
      }
      response = client.sendAsync(request.build(), HttpResponse.BodyHandlers.ofLines());
      Thread.ofVirtual()
          .start(
              () -> {
                try {
                  response.get().body().forEach(lines::add);
                } catch (Exception closed) {
                  // The stream ended or the client was shut down: nothing more to read.
                }
              });
    }

    int status() throws Exception {
      return response.get(10, TimeUnit.SECONDS).statusCode();
    }

    String cacheControl() throws Exception {
      return response.get(10, TimeUnit.SECONDS).headers().firstValue("Cache-Control").orElse("");
    }

    String awaitLine(Predicate<String> match, int seconds) throws InterruptedException {
      long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
      while (System.nanoTime() < end) {
        String line = lines.poll(100, TimeUnit.MILLISECONDS);
        if (line != null && match.test(line)) {
          return line;
        }
      }
      throw new AssertionError("no matching line within " + seconds + "s");
    }

    void close() {
      client.shutdownNow();
    }
  }
}
