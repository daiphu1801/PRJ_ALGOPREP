package com.algoprep.problembank.infrastructure.persistence;

import static com.algoprep.problembank.infrastructure.persistence.FakeJdbc.row;
import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Problem spec persistence (F2 spec endpoints): lock, upsert binding, outbox envelope. */
class JdbcProblemSpecRepositoryTest {
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

  private final FakeJdbc db = new FakeJdbc();
  private final UUID generated = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
  private final JdbcProblemSpecRepository repo =
      new JdbcProblemSpecRepository(db.provider(), () -> generated);

  @Test
  void lockMapsWriteStateAndIsEmptyForMissingProblem() {
    assertThat(repo.lock(P)).isEmpty();
    db.onQuery(
        "FOR UPDATE", row("author_id", ACTOR, "status", "DRAFT", "updated_at", Instant.EPOCH));

    var state = repo.lock(P).orElseThrow();

    assertThat(state.authorId()).isEqualTo(ACTOR);
    assertThat(state.status()).isEqualTo("DRAFT");
    assertThat(state.updatedAt()).isEqualTo(Instant.EPOCH);
  }

  @Test
  void findMapsAllSpecColumnsOrEmpty() {
    assertThat(repo.find(P)).isEmpty();
    db.onQuery(
        "FROM problem.problem_specs",
        row(
            "matching_strategy", "EXACT",
            "epsilon_value", null,
            "stdin_format_md", "in",
            "stdout_format_md", "out",
            "function_name", "f",
            "return_type", "{\"t\":1}",
            "parameters", "[]",
            "name_overrides", null,
            "spec_version", 3));

    var spec = repo.find(P).orElseThrow();

    assertThat(spec.specVersion()).isEqualTo(3);
    assertThat(spec.data().epsilonValue()).isNull();
    assertThat(spec.data().returnTypeJson()).isEqualTo("{\"t\":1}");
    assertThat(spec.data().nameOverridesJson()).isNull();
  }

  @Test
  void saveBindsEveryColumnWithGeneratedId() {
    var data =
        new ProblemSpecData(
            "FLOAT_TOLERANCE", new BigDecimal("0.5"), "i", "o", "fn", "{}", "[]", "{}");

    repo.save(P, new StoredProblemSpec(data, 4));

    var call = db.last("INSERT INTO problem.problem_specs");
    assertThat(call.param("id")).isEqualTo(generated);
    assertThat(call.param("problem")).isEqualTo(P);
    assertThat(call.param("matching")).isEqualTo("FLOAT_TOLERANCE");
    assertThat(call.param("epsilon")).isEqualTo(new BigDecimal("0.5"));
    assertThat(call.param("version")).isEqualTo(4);
    assertThat(call.param("parameters")).isEqualTo("[]");
  }

  @Test
  void touchReturnsNewUpdatedAtAndBindsWrapperFlag() {
    Instant stamp = Instant.parse("2026-07-01T00:00:00Z");
    db.onQuery("RETURNING updated_at", row("updated_at", stamp));

    assertThat(repo.touch(P, ACTOR, true)).isEqualTo(stamp);
    assertThat(db.last("RETURNING updated_at").param("supported")).isEqualTo(true);
  }

  @Test
  void appendEventWritesTheEnvelopeToOutbox() {
    repo.appendEvent(P, "{\"eventId\":\"x\"}");

    var call = db.last("INSERT INTO problem.outbox_events");
    assertThat(call.param("envelope")).isEqualTo("{\"eventId\":\"x\"}");
    assertThat(call.param("problem")).isEqualTo(P);
    assertThat(call.sql()).contains("'problem.spec.changed'");
  }
}
