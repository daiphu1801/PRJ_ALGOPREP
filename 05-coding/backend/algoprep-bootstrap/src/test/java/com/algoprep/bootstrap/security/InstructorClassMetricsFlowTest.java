package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Class metric reads #26, #28, #29 and #56 to #59 (DD api F1 sections 0.4.2, 0.8.2, 0.8.3) against
 * real PostgreSQL and Redis with real tokens. The assignments come through the real problem-bank
 * bridge and the figures through the real judge bridge (F4 P1), so the data is seeded in {@code
 * problem.class_assignments} and {@code judge.submissions}.
 *
 * <p>Class A has four students (one on track, one absent for ten days, one who never submitted, one
 * who only failed), class B is empty, class D holds one student whose accepted ratio fell two
 * completed weeks in a row.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.data.redis.timeout=300ms",
      "spring.data.redis.connect-timeout=300ms",
      "spring.datasource.hikari.maximum-pool-size=8"
    })
@ActiveProfiles("test")
@Testcontainers
class InstructorClassMetricsFlowTest {
  private static final String PASSWORD = "TempPass123";
  private static final String HASH = new BCryptPasswordEncoder().encode(PASSWORD);
  private static final String ID = "/api/v1/identity";

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

  @Autowired private WebApplicationContext context;
  @Autowired private JdbcTemplate jdbc;
  private MockMvc mvc;

  private UUID instructor;
  private String token;
  private UUID classA;
  private UUID classB;
  private UUID classD;
  private UUID an;
  private UUID binh;
  private UUID chi;
  private UUID dan;
  private UUID eli;
  private UUID p1;
  private UUID p2;

  @BeforeEach
  void world() throws Exception {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("DELETE FROM judge.submissions");
    jdbc.update("DELETE FROM problem.class_assignments");
    jdbc.update("DELETE FROM identity.class_enrollments");
    jdbc.update("DELETE FROM identity.classes");
    String email = account("INSTRUCTOR", "Teacher");
    instructor = id(email);
    token = login(email);
    an = id(account("STUDENT", "An"));
    binh = id(account("STUDENT", "Binh"));
    chi = id(account("STUDENT", "Chi"));
    dan = id(account("STUDENT", "Dan"));
    eli = id(account("STUDENT", "Eli"));
    p1 = problem("P1");
    p2 = problem("P2");
    UUID p3 = problem("P3");

    classA = clazz(instructor, "Class A");
    classB = clazz(instructor, "Class B");
    classD = clazz(instructor, "Class D");
    for (UUID s : List.of(an, binh, chi, dan)) {
      enrol(classA, s);
    }
    enrol(classD, eli);
    for (UUID c : List.of(classA, classD)) {
      give(p2, c, "61 days", null);
      give(p1, c, "60 days", null);
    }
    // A problem given and taken back does not count for anyone.
    give(p3, classA, "59 days", "58 days");

    submit(an, p1, "ACCEPTED", "1", "20 minutes");
    submit(an, p2, "WRONG_ANSWER", "0.5", "10 minutes");
    submit(binh, p1, "ACCEPTED", "1", "10 days");
    submit(dan, p1, "WRONG_ANSWER", "0", "30 minutes");
    // Self-practice on a problem the class was never given does not leave Chi insufficient.
    submit(chi, problem("SELF"), "ACCEPTED", "1", "5 minutes");
    // Eli: two accepted two weeks back, one accepted and one failed last week, one failure now.
    submit(eli, p1, "ACCEPTED", "1", weeksBackWednesday(2));
    submit(eli, p2, "ACCEPTED", "1", weeksBackWednesday(2));
    submit(eli, p1, "ACCEPTED", "1", weeksBackWednesday(1));
    submit(eli, p2, "WRONG_ANSWER", "0.5", weeksBackWednesday(1));
    submit(eli, p1, "WRONG_ANSWER", "0.5", "1 minute");
  }

  @Test
  void everyEndpointRefusesAnonymousAndStudentCallers() throws Exception {
    String student = login(account("STUDENT", "Other"));
    for (String path : allPaths()) {
      assertThat(mvc.perform(get(path)).andReturn().getResponse().getStatus()).isEqualTo(401);
      assertThat(call(path, student).getStatus()).isEqualTo(403);
    }
  }

  @Test
  void summaryCountsPeopleOnceAndMeansCompletionOverEveryStudent() throws Exception {
    String body = call(ID + "/instructor/classes/summary", token).getContentAsString();

    assertThat(JsonPath.<Integer>read(body, "$.classCount")).isEqualTo(3);
    assertThat(JsonPath.<Integer>read(body, "$.studentCount")).isEqualTo(5);
    assertThat(JsonPath.<Integer>read(body, "$.absentStudentCount")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.avgCompletionPercent")).isEqualTo(40);
    // Newest class first; the empty class has no figures, never a 0.
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].name"))
        .containsExactly("Class D", "Class B", "Class A");
    assertThat(JsonPath.<Object>read(body, "$.items[1].completionPercent")).isNull();
    assertThat(JsonPath.<Object>read(body, "$.items[1].avgScore")).isNull();
    assertThat(JsonPath.<Integer>read(body, "$.items[1].studentCount")).isZero();
    assertThat(JsonPath.<Integer>read(body, "$.items[2].completionPercent")).isEqualTo(25);
    assertThat(JsonPath.<Double>read(body, "$.items[2].avgScore")).isEqualTo(5.0);
    assertThat(JsonPath.<Integer>read(body, "$.items[2].absentCount")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.items[0].completionPercent")).isEqualTo(100);
  }

  @Test
  void aStudentInTwoClassesIsOnePersonInTheSummaryButOneRowPerClass() throws Exception {
    // An sits in Class A already; a second class makes the cards add up to 6 while 5 people exist.
    enrol(classB, an);

    String body = call(ID + "/instructor/classes/summary", token).getContentAsString();

    assertThat(JsonPath.<Integer>read(body, "$.studentCount")).isEqualTo(5);
    assertThat(JsonPath.<List<Integer>>read(body, "$.items[*].studentCount"))
        .containsExactly(1, 1, 4);
  }

  @Test
  void aTeacherWithoutClassesGetsEmptyHonestShapes() throws Exception {
    String lonely = login(account("INSTRUCTOR", "Lonely"));

    String summary = call(ID + "/instructor/classes/summary", lonely).getContentAsString();
    String trend = call(ID + "/instructor/classes/score-trend", lonely).getContentAsString();
    String attention =
        call(ID + "/instructor/classes/attention-students", lonely).getContentAsString();
    String page = call(ID + "/instructor/students", lonely).getContentAsString();

    assertThat(JsonPath.<Integer>read(summary, "$.classCount")).isZero();
    assertThat(JsonPath.<Object>read(summary, "$.avgCompletionPercent")).isNull();
    assertThat(JsonPath.<List<Object>>read(trend, "$.points")).hasSize(6);
    assertThat(JsonPath.<List<Object>>read(trend, "$.points[*].avgScore")).containsOnlyNulls();
    assertThat(JsonPath.<List<Object>>read(attention, "$.items")).isEmpty();
    assertThat(JsonPath.<Integer>read(page, "$.page.totalCount")).isZero();
  }

  @Test
  void attentionRanksAbsentThenNeedsSupportThenWatchWithTheNumbersBehindThem() throws Exception {
    String body = call(ID + "/instructor/classes/attention-students", token).getContentAsString();

    assertThat(JsonPath.<List<String>>read(body, "$.items[*].displayName"))
        .containsExactly("Binh", "Eli", "Dan");
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].status"))
        .containsExactly("ABSENT", "NEEDS_SUPPORT", "WATCH");
    assertThat(JsonPath.<List<String>>read(body, "$.items[*].reasonCode"))
        .containsExactly("ABSENT_DAYS", "SCORE_DROP_WEEKS", "LOW_COMPLETION_PERCENT");
    assertThat(JsonPath.<List<Integer>>read(body, "$.items[*].reasonValue"))
        .containsExactly(10, 2, 0);
    String empty =
        call(ID + "/instructor/classes/attention-students?classId=" + classB, token)
            .getContentAsString();
    assertThat(JsonPath.<List<Object>>read(empty, "$.items")).isEmpty();
  }

  @Test
  void scoreTrendIsSixCompletedWeeksWithNullForWeeksWithoutSubmissions() throws Exception {
    String body =
        call(ID + "/instructor/classes/score-trend?classId=" + classD, token).getContentAsString();

    assertThat(JsonPath.<String>read(body, "$.className")).isEqualTo("Class D");
    assertThat(JsonPath.<List<Object>>read(body, "$.points")).hasSize(6);
    assertThat(JsonPath.<List<Double>>read(body, "$.points[*].avgScore"))
        .containsExactly(null, null, null, null, 10.0, 5.0);
    String all = call(ID + "/instructor/classes/score-trend", token).getContentAsString();
    assertThat(JsonPath.<Object>read(all, "$.classId")).isNull();
    assertThat(JsonPath.<List<Object>>read(all, "$.points")).hasSize(6);
  }

  @Test
  void studentListIsSortedPagedSearchedAndScopedByClass() throws Exception {
    String first = call(ID + "/instructor/students?pageSize=2", token).getContentAsString();

    assertThat(JsonPath.<List<String>>read(first, "$.items[*].displayName"))
        .containsExactly("An", "Binh");
    assertThat(JsonPath.<Integer>read(first, "$.page.totalCount")).isEqualTo(5);
    assertThat(JsonPath.<Integer>read(first, "$.page.totalPages")).isEqualTo(3);
    assertThat(JsonPath.<String>read(first, "$.items[1].status")).isEqualTo("ABSENT");
    assertThat(JsonPath.<Integer>read(first, "$.items[1].completionPercent")).isEqualTo(50);
    assertThat(JsonPath.<Double>read(first, "$.items[1].avgScore")).isEqualTo(10.0);
    String last = call(ID + "/instructor/students?pageSize=2&page=3", token).getContentAsString();
    assertThat(JsonPath.<List<String>>read(last, "$.items[*].displayName")).containsExactly("Eli");
    assertThat(JsonPath.<List<Integer>>read(last, "$.items[0].trendPoints")).hasSize(6);
    String searched = call(ID + "/instructor/students?q=h", token).getContentAsString();
    assertThat(JsonPath.<List<String>>read(searched, "$.items[*].displayName"))
        .containsExactly("Binh", "Chi");
    String scoped = call(ID + "/instructor/students?classId=" + classD, token).getContentAsString();
    assertThat(JsonPath.<List<String>>read(scoped, "$.items[*].className"))
        .containsExactly("Class D");
    String chiRow = call(ID + "/instructor/students?q=chi", token).getContentAsString();
    assertThat(JsonPath.<String>read(chiRow, "$.items[0].status")).isEqualTo("INSUFFICIENT_DATA");
    assertThat(JsonPath.<Object>read(chiRow, "$.items[0].avgScore")).isNull();
    assertThat(JsonPath.<Object>read(chiRow, "$.items[0].lastSubmittedAt")).isNull();
    assertThat(JsonPath.<Integer>read(chiRow, "$.items[0].completionPercent")).isZero();
  }

  @Test
  void badPagingIsIdtV17AndAForeignOrMalformedClassIsIdt414OnEveryClassFilter() throws Exception {
    for (String bad : List.of("pageSize=0", "pageSize=101", "page=0", "q=" + "x".repeat(101))) {
      MockHttpServletResponse r = call(ID + "/instructor/students?" + bad, token);
      assertThat(r.getStatus()).isEqualTo(400);
      assertThat(r.getContentAsString()).contains("IDT-V17");
    }
    UUID foreign = clazz(id(account("INSTRUCTOR", "Other")), "Foreign");
    for (String path :
        List.of(
            ID + "/instructor/students?classId=",
            ID + "/instructor/classes/score-trend?classId=",
            ID + "/instructor/classes/attention-students?classId=")) {
      for (String value : List.of(foreign.toString(), "not-a-uuid", UUID.randomUUID().toString())) {
        MockHttpServletResponse r = call(path + value, token);
        assertThat(r.getStatus()).isEqualTo(404);
        assertThat(r.getContentAsString()).contains("IDT-414");
      }
    }
  }

  @Test
  void profileShowsTheStudentStatusAndCurrentAssignmentsNewestFirst() throws Exception {
    String body =
        call(classPath(classA) + "/students/" + binh + "/detail", token).getContentAsString();

    assertThat(JsonPath.<String>read(body, "$.displayName")).isEqualTo("Binh");
    assertThat(JsonPath.<String>read(body, "$.className")).isEqualTo("Class A");
    assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("ABSENT");
    // P1 was given after P2, so it comes first; the withdrawn P3 is not listed.
    assertThat(JsonPath.<List<String>>read(body, "$.assignmentProgress[*].problemId"))
        .containsExactly(p1.toString(), p2.toString());
    assertThat(JsonPath.<String>read(body, "$.assignmentProgress[0].bestVerdict"))
        .isEqualTo("ACCEPTED");
    assertThat(JsonPath.<Object>read(body, "$.assignmentProgress[1].bestVerdict")).isNull();
    assertThat(JsonPath.<Object>read(body, "$.assignmentProgress[1].bestRatio")).isNull();
  }

  @Test
  void profileIs404ForAStudentWhoLeftIsInAnotherClassOrForAForeignClass() throws Exception {
    jdbc.update("UPDATE identity.class_enrollments SET left_at = now() WHERE student_id = ?", chi);

    for (String path :
        List.of(
            classPath(classA) + "/students/" + chi + "/detail",
            classPath(classA) + "/students/" + eli + "/detail",
            classPath(classA) + "/students/not-a-uuid/detail",
            classPath(UUID.randomUUID()) + "/students/" + an + "/detail",
            ID + "/classes/not-a-uuid/students/" + an + "/detail")) {
      MockHttpServletResponse r = call(path, token);
      assertThat(r.getStatus()).as(path).isEqualTo(404);
      assertThat(r.getContentAsString()).contains("IDT-414");
    }
    String stranger = login(account("INSTRUCTOR", "Stranger"));
    assertThat(call(classPath(classA) + "/progress", stranger).getStatus()).isEqualTo(404);
  }

  @Test
  void progressCountsTheFiveLabelsOfAClassAndRosterCarriesTheFigures() throws Exception {
    String body = call(classPath(classA) + "/progress", token).getContentAsString();

    assertThat(JsonPath.<Integer>read(body, "$.studentCount")).isEqualTo(4);
    assertThat(JsonPath.<Integer>read(body, "$.completionPercent")).isEqualTo(25);
    assertThat(JsonPath.<Double>read(body, "$.avgScore")).isEqualTo(5.0);
    assertThat(JsonPath.<Integer>read(body, "$.statusCounts.INSUFFICIENT_DATA")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.statusCounts.ABSENT")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.statusCounts.WATCH")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.statusCounts.ON_TRACK")).isEqualTo(1);
    assertThat(JsonPath.<Integer>read(body, "$.statusCounts.NEEDS_SUPPORT")).isZero();
    String empty = call(classPath(classB) + "/progress", token).getContentAsString();
    assertThat(JsonPath.<Object>read(empty, "$.completionPercent")).isNull();

    String roster = call(classPath(classA) + "/students", token).getContentAsString();
    assertThat(JsonPath.<List<String>>read(roster, "$.items[*].status"))
        .containsExactlyInAnyOrder("ON_TRACK", "ABSENT", "INSUFFICIENT_DATA", "WATCH");
    assertThat(
            JsonPath.<List<Integer>>read(
                roster, "$.items[?(@.displayName=='An')].completionPercent"))
        .containsExactly(50);
  }

  @Test
  void aJudgeFailureIs503WithoutDetailAndTheRosterKeepsItsStudents() throws Exception {
    // A class nobody has read yet, so the judge cache cannot hide the failure.
    UUID fresh = clazz(instructor, "Fresh");
    enrol(fresh, an);
    give(p1, fresh, "5 days", null);
    jdbc.execute("ALTER TABLE judge.submissions RENAME TO submissions_away");
    try {
      MockHttpServletResponse detail =
          call(classPath(fresh) + "/students/" + an + "/detail", token);
      MockHttpServletResponse progress = call(classPath(fresh) + "/progress", token);
      MockHttpServletResponse roster = call(classPath(fresh) + "/students", token);

      assertThat(detail.getStatus()).isEqualTo(503);
      assertThat(detail.getContentAsString()).contains("IDT-507").doesNotContain("submissions");
      assertThat(progress.getStatus()).isEqualTo(503);
      assertThat(roster.getStatus()).isEqualTo(200);
      assertThat(JsonPath.<Object>read(roster.getContentAsString(), "$.items[0].status")).isNull();
      assertThat(JsonPath.<String>read(roster.getContentAsString(), "$.items[0].displayName"))
          .isEqualTo("An");
    } finally {
      jdbc.execute("ALTER TABLE judge.submissions_away RENAME TO submissions");
    }
  }

  @Test
  void everyScopeWideReadIs503WithoutDetailWhenTheJudgeOrTheAssignmentsFail() throws Exception {
    // A fresh teacher and class that nobody has read, so no cache can hide the failure.
    String email = account("INSTRUCTOR", "Fresh teacher");
    String freshToken = login(email);
    UUID fresh = clazz(id(email), "Fresh");
    enrol(fresh, an);
    give(p1, fresh, "5 days", null);
    List<String> paths =
        List.of(
            ID + "/instructor/classes/summary",
            ID + "/instructor/classes/score-trend",
            ID + "/instructor/classes/attention-students",
            ID + "/instructor/students");

    for (String[] source :
        new String[][] {
          {"judge", "submissions"},
          {"problem", "class_assignments"}
        }) {
      String table = source[0] + "." + source[1];
      jdbc.execute("ALTER TABLE " + table + " RENAME TO " + source[1] + "_away");
      try {
        for (String path : paths) {
          MockHttpServletResponse response = call(path, freshToken);
          assertThat(response.getStatus()).as(table + " " + path).isEqualTo(503);
          assertThat(response.getContentAsString())
              .as(table + " " + path)
              .contains("IDT-507")
              .doesNotContain(source[1]);
        }
      } finally {
        jdbc.execute("ALTER TABLE " + table + "_away RENAME TO " + source[1]);
      }
    }
  }

  private List<String> allPaths() {
    return List.of(
        ID + "/instructor/classes/summary",
        ID + "/instructor/classes/score-trend",
        ID + "/instructor/classes/attention-students",
        ID + "/instructor/students",
        classPath(classA) + "/progress",
        classPath(classA) + "/students",
        classPath(classA) + "/students/" + an + "/detail");
  }

  private String classPath(UUID classId) {
    return ID + "/classes/" + classId;
  }

  /** Wednesday noon (UTC+7) of the ISO week the given number of weeks before this one. */
  private String weeksBackWednesday(int weeks) {
    return jdbc.queryForObject(
        "SELECT CAST(date_trunc('week', now() AT TIME ZONE 'Asia/Ho_Chi_Minh')"
            + " - make_interval(weeks => ?) + interval '2 days 12 hours' AS text)",
        String.class,
        weeks);
  }

  private UUID problem(String code) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,deleted,"
            + "time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,?,'statement',?,"
            + "(SELECT id FROM problem.problem_levels ORDER BY sort_order LIMIT 1),'PUBLISHED',"
            + "false,1000,256,64)",
        id,
        code + "-" + id,
        "Title " + code,
        instructor);
    return id;
  }

  private UUID clazz(UUID owner, String name) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.classes(id,instructor_id,name) VALUES (?,?,?)", id, owner, name);
    return id;
  }

  private void enrol(UUID classId, UUID student) {
    jdbc.update(
        "INSERT INTO identity.class_enrollments(id,class_id,student_id,joined_at)"
            + " VALUES (?,?,?,now() - interval '90 days')",
        UUID.randomUUID(),
        classId,
        student);
  }

  private void give(UUID problem, UUID classId, String ago, String removedAgo) {
    jdbc.update(
        "INSERT INTO problem.class_assignments(id,problem_id,class_id,assigned_by,assigned_at,"
            + "removed_at) VALUES (?,?,?,?,now() - CAST(? AS interval),"
            + "CASE WHEN CAST(? AS text) IS NULL THEN NULL ELSE now() - CAST(? AS interval) END)",
        UUID.randomUUID(),
        problem,
        classId,
        instructor,
        ago,
        removedAgo,
        removedAgo);
  }

  /** {@code when} is either an interval ago ("10 days") or an absolute local timestamp. */
  private void submit(UUID user, UUID problem, String status, String score, String when) {
    boolean absolute = when.contains("-");
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,score_ratio,testcase_set_version_at_grading,submitted_at,
          finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','x','EDITOR',?,?,1,
          CASE WHEN ? THEN CAST(CAST(? AS timestamp) AT TIME ZONE 'Asia/Ho_Chi_Minh' AS timestamptz)
               ELSE now() - CAST(? AS interval) END,
          CASE WHEN ? THEN CAST(CAST(? AS timestamp) AT TIME ZONE 'Asia/Ho_Chi_Minh' AS timestamptz)
               ELSE now() - CAST(? AS interval) END)
        """,
        UUID.randomUUID(),
        user,
        problem,
        status,
        new BigDecimal(score),
        absolute,
        when,
        when,
        absolute,
        when,
        when);
  }

  private String account(String roleCode, String displayName) {
    String email = UUID.randomUUID() + "@example.test";
    jdbc.update(
        "INSERT INTO identity.users (id,email,password_hash,display_name,role_id,status,"
            + "must_change_password) SELECT ?,?,?,?,id,'ACTIVE',false FROM identity.roles"
            + " WHERE code = ?",
        UUID.randomUUID(),
        email,
        HASH,
        displayName,
        roleCode);
    return email;
  }

  private UUID id(String email) {
    return jdbc.queryForObject("SELECT id FROM identity.users WHERE email = ?", UUID.class, email);
  }

  private String login(String email) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                post(ID + "/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return JsonPath.read(response.getContentAsString(), "$.accessToken");
  }

  private MockHttpServletResponse call(String path, String bearer) throws Exception {
    return mvc.perform(get(path).header("Authorization", "Bearer " + bearer))
        .andReturn()
        .getResponse();
  }
}
