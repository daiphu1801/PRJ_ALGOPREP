package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** AI context read: only published problems, per-language function name override. */
class JdbcProblemContextReadRepositoryTest {
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final FakeJdbc db = new FakeJdbc();
  private final JdbcProblemContextReadRepository repo =
      new JdbcProblemContextReadRepository(db.provider());

  private Map<String, Object> ctx(Object statement, Object functionName, Object override) {
    return row(
        "title", "Two Sum",
        "statement_md", statement,
        "constraints_md", "c",
        "display_name", "Easy",
        "known_optimal_complexity", "O(n)",
        "ai_brief", "brief",
        "no_full_code", true,
        "socratic_only", false,
        "function_name", functionName,
        "return_type", "int",
        "parameters", "[]",
        "override_name", override,
        "topics", new Object[] {"Array"});
  }

  @Test
  void invisibleProblemHasNoContext() {
    assertThat(repo.findVisible(P, "JAVA")).isEmpty();
  }

  @Test
  void overrideNameWinsOverSpecFunctionNameAndLanguageIsBound() {
    db.onQuery("FROM problem.problems p", ctx("stmt", "twoSum", "two_sum"));

    var view = repo.findVisible(P, "PYTHON").orElseThrow();

    assertThat(view.functionSignature().functionName()).isEqualTo("two_sum");
    assertThat(view.topics()).containsExactly("Array");
    assertThat(view.noFullCode()).isTrue();
    assertThat(view.difficulty()).isEqualTo("Easy");
    assertThat(db.last("FROM problem.problems p").param("lang")).isEqualTo("PYTHON");
  }

  @Test
  void blankOverrideFallsBackToSpecName() {
    db.onQuery("FROM problem.problems p", ctx("stmt", "twoSum", "  "));

    assertThat(repo.findVisible(P, "JAVA").orElseThrow().functionSignature().functionName())
        .isEqualTo("twoSum");
  }

  @Test
  void stdIoProblemWithoutSpecHasNoSignatureAndNullStatementBecomesEmpty() {
    db.onQuery("FROM problem.problems p", ctx(null, null, null));

    var view = repo.findVisible(P, "JAVA").orElseThrow();

    assertThat(view.functionSignature()).isNull();
    assertThat(view.statementMarkdown()).isEmpty();
  }
}
