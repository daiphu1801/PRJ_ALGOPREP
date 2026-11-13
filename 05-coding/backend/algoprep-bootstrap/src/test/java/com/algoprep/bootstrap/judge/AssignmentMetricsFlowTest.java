package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.algoprep.judge.application.dto.AssignedProblemMetricsQuery;
import com.algoprep.judge.application.ports.in.GetAssignedProblemMetricsUseCase;
import com.algoprep.judge.application.ports.in.GetMyClassAssignmentStatesUseCase;
import com.algoprep.judge.application.ports.out.ClassAssignmentReadPort;
import com.algoprep.judge.application.ports.out.ClassScopeReadPort;
import com.algoprep.judge.domain.exception.AssignmentMetricsUnavailableException;
import com.algoprep.judge.domain.model.AssignmentPeriod;
import com.algoprep.judge.domain.model.AssignmentSolveState;
import com.algoprep.judge.domain.model.ClassProblemMetric;
import com.algoprep.judge.domain.ports.out.AssignmentMetricsRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/** P6/P7 through real PG and F1 scope bridge. No F2 callback or HTTP contract invented. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AssignmentMetricsFlowTest {
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
  static void infrastructure(DynamicPropertyRegistry p) {
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired GetAssignedProblemMetricsUseCase p6;
  @Autowired GetMyClassAssignmentStatesUseCase p7;
  @MockitoSpyBean ClassScopeReadPort scope;
  @MockitoSpyBean ClassAssignmentReadPort assignments;
  @MockitoSpyBean AssignmentMetricsRepository repository;
  private UUID instructor;
  private UUID student;
  private UUID classA;
  private UUID classB;
  private UUID problem;
  private Instant start;

  @BeforeEach
  void fixture() {
    SecurityContextHolder.clearContext();
    instructor = user("INSTRUCTOR");
    student = user("STUDENT");
    classA = UUID.fromString("7fffffff-0000-0000-0000-000000000001");
    classB = UUID.fromString("80000000-0000-0000-0000-000000000001");
    problem = UUID.randomUUID();
    start =
        jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class)
            .toInstant()
            .minusSeconds(86400);
    clazz(classA, instructor);
    clazz(classB, instructor);
    enroll(classA, student, start, null);
    enroll(classB, student, start, null);
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Assignment user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private void clazz(UUID id, UUID owner) {
    jdbc.update(
        "INSERT INTO identity.classes(id,instructor_id,name) VALUES(?,?,'Assignment class')",
        id,
        owner);
  }

  private void enroll(UUID classId, UUID who, Instant joined, Instant left) {
    jdbc.update(
        "INSERT INTO identity.class_enrollments(id,class_id,student_id,joined_at,left_at) VALUES(?,?,?,CAST(? AS timestamptz),CAST(? AS timestamptz))",
        UUID.randomUUID(),
        classId,
        who,
        joined.toString(),
        left == null ? null : left.toString());
  }

  private void submission(UUID owner, UUID p, Instant at, String status) {
    boolean terminal = !List.of("PENDING", "COMPILING", "JUDGING").contains(status);
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,failure_reason,score_ratio,testcase_set_version_at_grading,submitted_at,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','private-source-secret','EDITOR',?,?,?,1,CAST(? AS timestamptz),CAST(? AS timestamptz))
        """,
        UUID.randomUUID(),
        owner,
        p,
        status,
        status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
        status.equals("ACCEPTED") ? java.math.BigDecimal.ONE : null,
        at.toString(),
        terminal ? at.toString() : null);
  }

  private AssignmentPeriod period(UUID classId, int from, Integer end) {
    return new AssignmentPeriod(
        classId, problem, start.plusSeconds(from), end == null ? null : start.plusSeconds(end));
  }

  private ClassProblemMetric metric(Set<UUID> selected, List<AssignmentPeriod> periods) {
    return p6.read(new AssignedProblemMetricsQuery(instructor, selected, Set.of(problem), periods))
        .get(problem);
  }

  private void principal(UUID who, String role) {
    var jwt = Jwt.withTokenValue("test").header("alg", "none").subject(who.toString()).build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  @Test
  void p6AttributesAcrossEveryClassBeforeFilteringAndCountsOnlyEligibleVerdictsInTheRate() {
    var periods = List.of(period(classA, 0, null), period(classB, 10, null));
    submission(student, problem, start.plusSeconds(5), "ACCEPTED");
    submission(student, problem, start.plusSeconds(15), "WRONG_ANSWER");
    submission(student, problem, start.plusSeconds(20), "SYSTEM_ERROR");
    submission(student, problem, start.plusSeconds(25), "PENDING");
    assertThat(metric(Set.of(classA), periods)).isEqualTo(new ClassProblemMetric(1, 100.0));
    assertThat(metric(Set.of(classB), periods)).isEqualTo(new ClassProblemMetric(3, 0.0));
    assertThat(metric(Set.of(classA, classB), periods)).isEqualTo(new ClassProblemMetric(4, 50.0));
    verify(scope, times(3)).read(instructor);
    verify(assignments, never()).listAssignments(any());
  }

  @Test
  void p6FiltersCurrentMembersEnrollmentStartAndExclusiveRemovalIncludingHistoricalAssignments() {
    UUID former = user("STUDENT");
    UUID stranger = user("STUDENT");
    UUID late = user("STUDENT");
    enroll(classA, former, start, start.plusSeconds(50));
    enroll(classA, late, start.plusSeconds(20), null);
    submission(former, problem, start.plusSeconds(10), "ACCEPTED");
    submission(stranger, problem, start.plusSeconds(10), "ACCEPTED");
    submission(late, problem, start.plusSeconds(19), "ACCEPTED");
    submission(late, problem, start.plusSeconds(20), "WRONG_ANSWER");
    submission(student, problem, start.minusSeconds(1), "ACCEPTED");
    submission(student, problem, start.plusSeconds(9), "ACCEPTED");
    submission(student, problem, start.plusSeconds(10), "ACCEPTED");
    submission(student, problem, start.plusSeconds(49), "ACCEPTED");
    submission(student, problem, start.plusSeconds(50), "ACCEPTED");
    var periods = List.of(period(classA, 10, 50));
    assertThat(metric(Set.of(classA), periods)).isEqualTo(new ClassProblemMetric(3, 200.0 / 3));
  }

  @Test
  void p6BreaksEqualAssignmentTiesByPostgresUuidAndDoesNotDuplicateRepeatedIntervals() {
    var first = period(classA, 0, null);
    var second = period(classB, 0, null);
    submission(student, problem, start, "ACCEPTED");
    assertThat(metric(Set.of(classA), List.of(first, second, first)))
        .isEqualTo(new ClassProblemMetric(1, 100.0));
    assertThat(metric(Set.of(classB), List.of(first, second)))
        .isEqualTo(new ClassProblemMetric(0, null));
  }

  @Test
  void p6ReturnsAllZeroKeysForEmptyOwnedClassesAndRejectsForeignScope() {
    UUID empty = UUID.randomUUID();
    clazz(empty, instructor);
    UUID untouched = UUID.randomUUID();
    var result =
        p6.read(
            new AssignedProblemMetricsQuery(
                instructor, Set.of(empty), Set.of(problem, untouched), List.of()));
    assertThat(result)
        .hasSize(2)
        .allSatisfy((id, value) -> assertThat(value).isEqualTo(new ClassProblemMetric(0, null)));
    assertThat(metric(Set.of(), List.of())).isEqualTo(new ClassProblemMetric(0, null));
    UUID foreign = UUID.randomUUID();
    clazz(foreign, user("INSTRUCTOR"));
    assertThatThrownBy(() -> metric(Set.of(foreign), List.of()))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasNoCause();
    assertThatThrownBy(() -> metric(Set.of(classA), List.of(period(foreign, 0, null))))
        .isInstanceOf(AssignmentMetricsUnavailableException.class);
  }

  @Test
  void p7UsesOwnHistoryAndExactAssignmentBoundsRatherThanGlobalAcceptedState() {
    principal(student, "STUDENT");
    UUID other = user("STUDENT");
    var old = period(classA, 0, 10);
    var active = period(classA, 20, null);
    var another = period(classB, 10, 20);
    submission(student, problem, start.minusSeconds(1), "ACCEPTED");
    submission(student, problem, start, "ACCEPTED");
    submission(student, problem, start.plusSeconds(10), "SYSTEM_ERROR");
    submission(student, problem, start.plusSeconds(20), "PENDING");
    submission(other, problem, start.plusSeconds(25), "ACCEPTED");
    var result = p7.read(List.of(old, active, another, old));
    assertThat(result)
        .hasSize(3)
        .containsEntry(old.key(), AssignmentSolveState.SOLVED)
        .containsEntry(active.key(), AssignmentSolveState.ATTEMPTED)
        .containsEntry(another.key(), AssignmentSolveState.ATTEMPTED);
    var empty = period(classB, 30, 40);
    submission(student, problem, start.plusSeconds(40), "ACCEPTED");
    assertThat(p7.read(List.of(empty))).containsEntry(empty.key(), AssignmentSolveState.UNSOLVED);
    verify(scope, never()).read(any());
    verify(assignments, never()).listAssignments(any());
  }

  @Test
  void p7RejectsNonStudentsEvenForEmptyInputAndReturnsEmptyOrUnsolvedOnlyOnSuccessfulReads() {
    assertThatThrownBy(() -> p7.read(List.of())).isInstanceOf(AccessDeniedException.class);
    principal(student, "ADMIN");
    assertThatThrownBy(() -> p7.read(List.of(period(classA, 0, null))))
        .isInstanceOf(AccessDeniedException.class);
    principal(student, "STUDENT");
    assertThat(p7.read(List.of())).isEmpty();
    var period = period(classA, 0, null);
    assertThat(p7.read(List.of(period))).containsEntry(period.key(), AssignmentSolveState.UNSOLVED);
  }

  @Test
  void dependencyErrorsAreSanitizedAndReadsDoNotWriteOrExposeSource() {
    var period = period(classA, 0, null);
    long before = jdbc.queryForObject("SELECT count(*) FROM judge.submissions", Long.class);
    String serialized =
        JsonMapper.builder().build().writeValueAsString(metric(Set.of(classA), List.of(period)));
    assertThat(serialized)
        .doesNotContain("private-source-secret", "sourceCode", "testcase", "studentId");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM judge.submissions", Long.class))
        .isEqualTo(before);
    doThrow(new IllegalStateException("private F1 SQL password")).when(scope).read(instructor);
    assertThatThrownBy(() -> metric(Set.of(classA), List.of(period)))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasMessage("Assignment metrics unavailable")
        .hasNoCause();
    principal(student, "STUDENT");
    doThrow(new IllegalStateException("private database password"))
        .when(repository)
        .assignmentStates(student, List.of(period));
    assertThatThrownBy(() -> p7.read(List.of(period)))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasNoCause();
  }
}
