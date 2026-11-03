package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.ProblemWriteState;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import com.algoprep.problembank.domain.ports.out.ProblemSpecRepository;
import java.sql.Types;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProblemSpecRepository implements ProblemSpecRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;

  JdbcProblemSpecRepository(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  @Override
  public Optional<ProblemWriteState> lock(UUID problemId) {
    return jdbc.getObject()
        .sql(
            "SELECT author_id,status,updated_at FROM problem.problems WHERE id=:id AND NOT deleted FOR UPDATE")
        .param("id", problemId)
        .query(
            (rs, row) ->
                new ProblemWriteState(
                    rs.getObject("author_id", UUID.class),
                    rs.getString("status"),
                    rs.getTimestamp("updated_at").toInstant()))
        .optional();
  }

  @Override
  public Optional<StoredProblemSpec> find(UUID problemId) {
    return jdbc.getObject()
        .sql("SELECT * FROM problem.problem_specs WHERE problem_id=:id")
        .param("id", problemId)
        .query(
            (rs, row) ->
                new StoredProblemSpec(
                    new ProblemSpecData(
                        rs.getString("matching_strategy"),
                        rs.getBigDecimal("epsilon_value"),
                        rs.getString("stdin_format_md"),
                        rs.getString("stdout_format_md"),
                        rs.getString("function_name"),
                        rs.getString("return_type"),
                        rs.getString("parameters"),
                        rs.getString("name_overrides")),
                    rs.getInt("spec_version")))
        .optional();
  }

  @Override
  public void save(UUID problemId, StoredProblemSpec spec) {
    var data = spec.data();
    jdbc.getObject()
        .sql(
            """
        INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,epsilon_value,stdin_format_md,stdout_format_md,function_name,return_type,parameters,name_overrides,spec_version,updated_at)
        VALUES (:id,:problem,:matching,:epsilon,:stdin,:stdout,:name,CAST(:return AS jsonb),CAST(:parameters AS jsonb),CAST(:overrides AS jsonb),:version,clock_timestamp())
        ON CONFLICT (problem_id) DO UPDATE SET matching_strategy=EXCLUDED.matching_strategy,epsilon_value=EXCLUDED.epsilon_value,
          stdin_format_md=EXCLUDED.stdin_format_md,stdout_format_md=EXCLUDED.stdout_format_md,function_name=EXCLUDED.function_name,
          return_type=EXCLUDED.return_type,parameters=EXCLUDED.parameters,name_overrides=EXCLUDED.name_overrides,spec_version=EXCLUDED.spec_version,updated_at=EXCLUDED.updated_at
        """)
        .param("id", ids.newId())
        .param("problem", problemId)
        .param("matching", data.matchingStrategy())
        .param("epsilon", data.epsilonValue(), Types.NUMERIC)
        .param("stdin", data.stdinFormatMd(), Types.VARCHAR)
        .param("stdout", data.stdoutFormatMd(), Types.VARCHAR)
        .param("name", data.functionName(), Types.VARCHAR)
        .param("return", data.returnTypeJson(), Types.VARCHAR)
        .param("parameters", data.parametersJson(), Types.VARCHAR)
        .param("overrides", data.nameOverridesJson(), Types.VARCHAR)
        .param("version", spec.specVersion())
        .update();
  }

  @Override
  public Instant touch(UUID problemId, UUID actorId, boolean wrapperSupported) {
    return jdbc.getObject()
        .sql(
            """
        UPDATE problem.problems SET function_wrapper_supported=:supported,updated_by=:actor,
          updated_at=GREATEST(clock_timestamp(),updated_at + interval '1 microsecond')
        WHERE id=:id RETURNING updated_at
        """)
        .param("supported", wrapperSupported)
        .param("actor", actorId)
        .param("id", problemId)
        .query((rs, row) -> rs.getTimestamp("updated_at").toInstant())
        .single();
  }

  @Override
  public void appendEvent(UUID problemId, String envelope) {
    jdbc.getObject()
        .sql(
            """
        INSERT INTO problem.outbox_events(id,aggregate_type,aggregate_id,event_type,routing_key,payload)
        VALUES (CAST(CAST(:envelope AS jsonb)->>'eventId' AS uuid),'Problem',:problem,'ProblemSpecChanged','problem.spec.changed',CAST(:envelope AS jsonb))
        """)
        .param("envelope", envelope)
        .param("problem", problemId)
        .update();
  }
}
