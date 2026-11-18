package com.algoprep.problembank.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.domain.model.problem.ProblemContentData;
import com.algoprep.problembank.domain.model.problem.ProblemContentData.Example;
import com.algoprep.problembank.domain.model.problem.ProblemContentData.Limits;
import com.algoprep.problembank.domain.model.problem.ProblemContentData.Solution;
import com.algoprep.problembank.domain.model.problem.ProblemSpecData;
import com.algoprep.problembank.domain.model.problem.StoredProblemSpec;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Problem content, relations, specification and the authoring read model on real PostgreSQL. */
class ProblemContentSpecPgTest extends PostgresTestBase {
  private JdbcProblemContentRepository content;
  private JdbcProblemAuthoringReadRepository authoring;
  private JdbcProblemSpecRepository specs;
  private JdbcHarnessProblemReadRepository harness;
  private JdbcProblemContextReadRepository context;

  @BeforeEach
  void seed() {
    var provider = provider();
    var relations = new JdbcProblemContentRelations(provider, UUID::randomUUID);
    content = new JdbcProblemContentRepository(provider, UUID::randomUUID, relations);
    authoring = new JdbcProblemAuthoringReadRepository(provider, relations);
    specs = new JdbcProblemSpecRepository(provider, UUID::randomUUID);
    harness = new JdbcHarnessProblemReadRepository(provider);
    context = new JdbcProblemContextReadRepository(provider);
  }

  private static ProblemContentData draft(String title) {
    return new ProblemContentData(
        title,
        null,
        "",
        EASY,
        new Limits(1000, 256, 64, null),
        List.of(),
        List.of(),
        List.of(),
        null);
  }

  private static ProblemSpecData signature(String strategy, BigDecimal epsilon) {
    return new ProblemSpecData(
        strategy,
        epsilon,
        "stdin",
        "stdout",
        "twoSum",
        "\"int\"",
        "[{\"name\":\"nums\",\"type\":\"int[]\"}]",
        "{\"PYTHON\":\"two_sum\"}");
  }

  @Test
  void createAllocatesAUniqueCodeEvenAcrossDeletedProblems() {
    UUID first = content.create(AUTHOR, draft("Hello World"));
    UUID second = content.create(AUTHOR, draft("Hello World"));
    softDelete(second);
    UUID third = content.create(AUTHOR, draft("hello world"));

    var codes =
        jdbc.sql("SELECT code FROM problem.problems ORDER BY code").query(String.class).list();
    assertThat(codes).containsExactly("HELLO-WORLD", "HELLO-WORLD-2", "HELLO-WORLD-3");
    assertThat(List.of(first, second, third)).doesNotHaveDuplicates();
    var row =
        jdbc.sql(
                "SELECT status, current_testcase_set_version v, no_full_code f FROM problem.problems WHERE id=:id")
            .param("id", first)
            .query((rs, n) -> rs.getString("status") + rs.getInt("v") + rs.getBoolean("f"))
            .single();
    assertThat(row).isEqualTo("UNPUBLISHED1true");
  }

  @Test
  void createRejectsAnUnknownLevelAndNonPositiveLimits() {
    var unknownLevel =
        new ProblemContentData(
            "T", null, "", id(999), new Limits(1000, 256, 64, null), null, null, null, null);
    var zeroTime =
        new ProblemContentData(
            "T", null, "", EASY, new Limits(0, 256, 64, null), null, null, null, null);

    assertThatThrownBy(() -> content.create(AUTHOR, unknownLevel))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> content.create(AUTHOR, zeroTime))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(count("problems")).isZero();
  }

  @Test
  void writeReplacesRelationsAndReusesTagsCaseInsensitively() {
    UUID p = content.create(AUTHOR, draft("Relations"));
    UUID other = content.create(AUTHOR, draft("Other"));
    var full =
        new ProblemContentData(
            "Relations v2",
            "# statement",
            "n <= 10",
            MEDIUM,
            new Limits(2000, 128, 32, 30),
            List.of(MANG, CHUOI),
            List.of("Graph", "dp"),
            List.of(new Example(null, 9, "1", "2", null), new Example(null, 7, "3", "4", "why")),
            new Solution("JAVA", "class A {}"));
    content.write(p, AUTHOR, full);
    content.write(
        other,
        AUTHOR,
        new ProblemContentData(
            "Other",
            null,
            "",
            EASY,
            new Limits(1000, 256, 64, null),
            List.of(),
            List.of("GRAPH"),
            List.of(),
            null));

    var snapshot = content.lock(p).orElseThrow();

    assertThat(count("tags")).isEqualTo(2);
    assertThat(snapshot.content().title()).isEqualTo("Relations v2");
    assertThat(snapshot.content().limits().maxSubmissionsPerHour()).isEqualTo(30);
    assertThat(snapshot.content().topicIds()).containsExactlyInAnyOrder(MANG, CHUOI);
    assertThat(snapshot.content().tags()).containsExactly("dp", "Graph");
    assertThat(snapshot.content().workedExamples())
        .extracting(Example::position)
        .containsExactly(1, 2);
    assertThat(snapshot.content().workedExamples().get(1).explanationMd()).isEqualTo("why");
    assertThat(snapshot.content().referenceSolution().language()).isEqualTo("JAVA");
    rewriteWithFewerRelations(p);
  }

  private void rewriteWithFewerRelations(UUID p) {
    content.write(
        p,
        AUTHOR,
        new ProblemContentData(
            "Relations v3",
            "s",
            "",
            MEDIUM,
            new Limits(2000, 128, 32, null),
            List.of(MANG),
            List.of(),
            List.of(),
            new Solution("CPP", "int main(){}")));
    var again = content.lock(p).orElseThrow();
    assertThat(again.content().topicIds()).containsExactly(MANG);
    assertThat(again.content().tags()).isEmpty();
    assertThat(again.content().workedExamples()).isEmpty();
    assertThat(again.content().referenceSolution().language()).isEqualTo("CPP");
    assertThat(count("problem_reference_solutions")).isEqualTo(1);
    assertThat(again.content().limits().maxSubmissionsPerHour()).isNull();

    content.write(
        p,
        AUTHOR,
        new ProblemContentData(
            "Relations v3",
            "s",
            "",
            MEDIUM,
            new Limits(2000, 128, 32, null),
            List.of(),
            List.of(),
            List.of(),
            null));
    assertThat(count("problem_reference_solutions")).isZero();
  }

  @Test
  void writeRejectsAReferenceSolutionInAnUnsupportedLanguage() {
    UUID p = content.create(AUTHOR, draft("Lang"));
    var bad =
        new ProblemContentData(
            "Lang",
            "s",
            "",
            EASY,
            new Limits(1000, 256, 64, null),
            List.of(),
            List.of(),
            List.of(),
            new Solution("RUBY", "x"));

    assertThatThrownBy(() -> content.write(p, AUTHOR, bad))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void lockSkipsDeletedProblemsAndCatalogueHelpersReadRealRows() {
    UUID p = content.create(AUTHOR, draft("Lockable"));

    assertThat(content.lock(p)).isPresent();
    softDelete(p);
    assertThat(content.lock(p)).isEmpty();
    assertThat(content.firstLevel()).contains(EASY);
    assertThat(content.cataloguesExist(EASY, List.of(MANG, CHUOI))).isTrue();
    assertThat(content.cataloguesExist(EASY, List.of(MANG, id(999)))).isFalse();
    assertThat(content.cataloguesExist(id(999), List.of())).isFalse();
    long locks =
        tx.execute(
            s -> {
              content.lockCatalogues();
              return jdbc.sql(
                      "SELECT count(*) FROM pg_locks WHERE locktype='advisory'"
                          + " AND objid IN (2201001,2201002) AND pid=pg_backend_pid() AND granted")
                  .query(Long.class)
                  .single();
            });
    assertThat(locks).isEqualTo(2);
  }

  @Test
  void aiContextAndDuplicateMarkerPersist() {
    UUID p = content.create(AUTHOR, draft("Ai"));
    UUID copy = content.create(AUTHOR, draft("Ai copy"));

    var at = content.writeAiContext(p, id(901), "brief", false, false);
    content.markDuplicatedFrom(copy, p);

    var record = authoring.find(p, false).orElseThrow();
    assertThat(record.updatedAt()).isEqualTo(at);
    assertThat(record.aiBrief()).isEqualTo("brief");
    assertThat(record.noFullCode()).isFalse();
    assertThat(record.updatedBy()).isEqualTo(id(901));
    assertThat(authoring.find(copy, false).orElseThrow().duplicatedFromProblemId()).isEqualTo(p);
    content.writeAiContext(p, id(901), null, true, true);
    assertThat(authoring.find(p, false).orElseThrow().aiBrief()).isNull();
  }

  @Test
  void authoringReadWithholdsTestcaseBodiesUnlessAskedFor() {
    UUID p = published(1, "AUTH");
    testcase(p, 100, "HIDDEN", true, at(1, "SECRET"));
    link(p, MANG);
    specs.save(p, new StoredProblemSpec(signature("EXACT", null), 1));

    var without = authoring.find(p, false).orElseThrow();
    var with = authoring.find(p, true).orElseThrow();

    assertThat(without.testcases()).hasSize(1);
    assertThat(without.testcases().get(0).inputInline()).isNull();
    assertThat(without.testcases().get(0).expectedInline()).isNull();
    assertThat(without.toString()).doesNotContain("SECRET");
    assertThat(with.testcases().get(0).inputInline()).isEqualTo("in-SECRET");
    assertThat(with.topicIds()).containsExactly(MANG);
    assertThat(with.spec().data().functionName()).isEqualTo("twoSum");
    assertThat(authoring.aiGenerationLimit()).isEqualTo(2);
    softDelete(p);
    assertThat(authoring.find(p, true)).isEmpty();
  }

  @Test
  void specSaveInsertsThenUpsertsAndRoundTripsJson() {
    UUID p = published(1, "SPEC");

    specs.save(p, new StoredProblemSpec(signature("EXACT", null), 1));
    specs.save(p, new StoredProblemSpec(signature("EPSILON", new BigDecimal("0.000001")), 2));

    var stored = specs.find(p).orElseThrow();
    assertThat(count("problem_specs")).isEqualTo(1);
    assertThat(stored.specVersion()).isEqualTo(2);
    assertThat(stored.data().matchingStrategy()).isEqualTo("EPSILON");
    assertThat(stored.data().epsilonValue()).isEqualByComparingTo("0.000001");
    assertThat(stored.data().returnTypeJson()).isEqualTo("\"int\"");
    assertThat(stored.data().nameOverridesJson()).contains("two_sum");
    assertThat(specs.find(id(999))).isEmpty();
  }

  @Test
  void specCheckConstraintsEnforceEpsilonAndSignatureRules() {
    UUID p = published(1, "SPECCK");
    var epsilonMissing = signature("EPSILON", null);
    var halfSignature = new ProblemSpecData("EXACT", null, null, null, "f", null, null, null);
    var badStrategy = signature("FUZZY", null);
    var zeroEpsilon = signature("EXACT", BigDecimal.ZERO);

    for (var invalid : List.of(epsilonMissing, halfSignature, badStrategy, zeroEpsilon)) {
      assertThatThrownBy(() -> specs.save(p, new StoredProblemSpec(invalid, 1)))
          .isInstanceOf(DataIntegrityViolationException.class);
    }
    specs.save(
        p,
        new StoredProblemSpec(
            new ProblemSpecData("TRIMMED", null, null, null, null, null, null, null), 1));
    assertThat(specs.find(p).orElseThrow().data().functionName()).isNull();
    assertThat(count("problem_specs")).isEqualTo(1);
  }

  @Test
  void specLockTouchAndEventUseTheRealRow() {
    UUID p = published(1, "SPECLOCK");
    var state = specs.lock(p).orElseThrow();

    var at = specs.touch(p, id(901), false);
    specs.appendEvent(p, "{\"eventId\":\"" + id(700) + "\"}");

    assertThat(at).isAfter(state.updatedAt());
    assertThat(
            jdbc.sql("SELECT function_wrapper_supported FROM problem.problems WHERE id=:id")
                .param("id", p)
                .query(Boolean.class)
                .single())
        .isFalse();
    assertThat(count("outbox_events", "event_type='ProblemSpecChanged'")).isEqualTo(1);
    softDelete(p);
    assertThat(specs.lock(p)).isEmpty();
  }

  @Test
  void harnessReadAndAccessMatrix() {
    UUID pub = published(1, "H-PUB");
    UUID draft = problem(2, "H-DRAFT", "Draft", "UNPUBLISHED", EASY);
    specs.save(pub, new StoredProblemSpec(signature("EXACT", null), 3));
    UUID student = id(800);

    var view = harness.findSpec(pub).orElseThrow();

    assertThat(view.specVersion()).isEqualTo(3);
    assertThat(view.functionWrapperSupported()).isTrue();
    assertThat(harness.findSpec(draft)).isEmpty();
    assertThat(harness.canRead(pub, student, "STUDENT")).isTrue();
    assertThat(harness.canRead(draft, student, "STUDENT")).isFalse();
    assertThat(harness.canRead(draft, AUTHOR, "INSTRUCTOR")).isTrue();
    assertThat(harness.canRead(draft, student, "ADMIN")).isTrue();
    softDelete(pub);
    assertThat(harness.canRead(pub, AUTHOR, "ADMIN")).isFalse();
  }

  @Test
  void aiContextReadExposesOnlyPublishedProblemsAndPerLanguageName() {
    UUID p = published(1, "CTX");
    UUID draft = problem(2, "CTX-DRAFT", "Draft", "UNPUBLISHED", EASY);
    link(p, MANG);
    specs.save(p, new StoredProblemSpec(signature("EXACT", null), 1));
    testcase(p, 100, "HIDDEN", true, at(1, "SECRET"));

    var python = context.findVisible(p, "PYTHON").orElseThrow();
    var java = context.findVisible(p, "JAVA").orElseThrow();

    assertThat(python.functionSignature().functionName()).isEqualTo("two_sum");
    assertThat(java.functionSignature().functionName()).isEqualTo("twoSum");
    assertThat(java.topics()).containsExactly("Mảng");
    assertThat(java.statementMarkdown()).isEmpty();
    assertThat(java.toString()).doesNotContain("SECRET");
    assertThat(context.findVisible(draft, "JAVA")).isEmpty();
    UUID noSpec = published(3, "CTX-NOSPEC");
    assertThat(context.findVisible(noSpec, "JAVA").orElseThrow().functionSignature()).isNull();
  }
}
