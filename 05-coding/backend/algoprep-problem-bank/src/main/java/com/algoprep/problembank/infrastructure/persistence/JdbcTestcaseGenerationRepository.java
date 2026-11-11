package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.TestcaseGenerationRun;
import com.algoprep.problembank.domain.ports.out.TestcaseGenerationRepository;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcTestcaseGenerationRepository implements TestcaseGenerationRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcTestcaseGenerationRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean consumeQuota(UUID problemId) {
    return jdbc.getObject()
            .sql(
                """
                UPDATE problem.problems
                SET ai_testcase_generations_used = ai_testcase_generations_used + 1
                WHERE id = :id
                  AND ai_testcase_generations_used <
                      (SELECT max_ai_generations_per_problem
                       FROM problem.problem_bank_settings WHERE id = 1)
                """)
            .param("id", problemId)
            .update()
        == 1;
  }

  @Override
  public void insertRun(TestcaseGenerationRun run) {
    jdbc.getObject()
        .sql(
            """
            INSERT INTO problem.testcase_generation_runs
              (id, problem_id, generator_script, validator_script, seed, model_id,
               prompt_version, requested_by, added_count, dropped_json)
            VALUES (:id, :problem, :generator, :validator, :seed, :model, :prompt, :by,
                    :added, CAST(:dropped AS jsonb))
            """)
        .param("id", run.id())
        .param("problem", run.problemId())
        .param("generator", run.generatorScript())
        .param("validator", run.validatorScript())
        .param("seed", run.seed())
        .param("model", run.modelId())
        .param("prompt", run.promptVersion())
        .param("by", run.requestedBy())
        .param("added", run.addedCount())
        .param("dropped", run.droppedJson())
        .update();
  }

  @Override
  public int lockCap() {
    return jdbc.getObject()
        .sql(
            "SELECT max_ai_generations_per_problem FROM problem.problem_bank_settings"
                + " WHERE id = 1 FOR UPDATE")
        .query(Integer.class)
        .single();
  }

  @Override
  public void updateCap(int maxPerProblem, UUID actorId) {
    jdbc.getObject()
        .sql(
            """
            UPDATE problem.problem_bank_settings
            SET max_ai_generations_per_problem = :max, updated_by = :by, updated_at = now()
            WHERE id = 1
            """)
        .param("max", maxPerProblem)
        .param("by", actorId)
        .update();
  }
}
