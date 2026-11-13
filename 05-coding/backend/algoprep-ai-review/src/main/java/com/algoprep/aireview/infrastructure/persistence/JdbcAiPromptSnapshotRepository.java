package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptSnapshot;
import com.algoprep.aireview.domain.model.ResponseGuards;
import com.algoprep.aireview.domain.ports.out.AiPromptSnapshotRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcAiPromptSnapshotRepository implements AiPromptSnapshotRepository {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAiPromptSnapshotRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<PromptSnapshot> readActive(FeatureCode featureCode) {
    return jdbc.getObject()
        .sql(
            "SELECT id, system_instruction, response_schema_version, model_id,"
                + " response_guards::text AS guards"
                + " FROM ai.prompt_templates WHERE feature_code = :feature AND status = 'ACTIVE'")
        .param("feature", featureCode.name())
        .query(
            (rs, row) ->
                new PromptSnapshot(
                    rs.getObject("id", UUID.class),
                    featureCode,
                    rs.getString("system_instruction"),
                    guards(rs.getString("guards")),
                    rs.getString("model_id"),
                    rs.getString("response_schema_version")))
        .optional();
  }

  private static ResponseGuards guards(String json) {
    if (json == null) {
      return ResponseGuards.NONE;
    }
    JsonNode node = JSON.readTree(json);
    return new ResponseGuards(
        node.path("citeLines").asBoolean(false),
        node.path("answerInVietnamese").asBoolean(false),
        node.path("noFullSolution").asBoolean(false));
  }
}
