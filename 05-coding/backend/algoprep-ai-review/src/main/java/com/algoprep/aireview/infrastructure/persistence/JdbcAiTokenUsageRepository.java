package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.TokenUsageRecord;
import com.algoprep.aireview.domain.ports.out.AiTokenUsageRepository;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAiTokenUsageRepository implements AiTokenUsageRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;

  JdbcAiTokenUsageRepository(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  @Override
  public boolean insertIfAbsent(TokenUsageRecord usage) {
    return jdbc.getObject()
            .sql(
                "INSERT INTO ai.token_usage (id, call_id, user_id, feature_code, problem_id,"
                    + " owner_id, tokens_prompt, tokens_completion, model_name)"
                    + " VALUES (:id, :call, :user, :feature, :problem, :owner, :prompt,"
                    + " :completion, :model) ON CONFLICT (call_id) DO NOTHING")
            .param("id", ids.newId())
            .param("call", usage.callId())
            .param("user", usage.userId())
            .param("feature", usage.featureCode().name())
            .param("problem", usage.problemId())
            .param("owner", usage.ownerId())
            .param("prompt", usage.tokensPrompt())
            .param("completion", usage.tokensCompletion())
            .param("model", usage.modelName())
            .update()
        == 1;
  }
}
