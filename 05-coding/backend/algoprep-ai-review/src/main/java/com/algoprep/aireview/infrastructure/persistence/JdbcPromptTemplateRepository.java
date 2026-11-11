package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.PromptContent;
import com.algoprep.aireview.domain.model.PromptStatus;
import com.algoprep.aireview.domain.model.PromptTemplate;
import com.algoprep.aireview.domain.model.ResponseGuards;
import com.algoprep.aireview.domain.ports.out.PromptTemplateRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Repository
class JdbcPromptTemplateRepository implements PromptTemplateRepository {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Pattern VERSION = Pattern.compile("^v(\\d+)\\.(\\d+)$");
  private static final String COLUMNS =
      "id, feature_code, version, status, system_instruction, model_id,"
          + " response_guards::text AS guards, response_schema_version, published_at, created_at";
  private static final String FEATURE_ORDER =
      "CASE feature_code WHEN 'SOLUTION_REVIEW' THEN 0 WHEN 'MOCK_INTERVIEW' THEN 1 ELSE 2 END";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcPromptTemplateRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<PromptTemplate> listLive() {
    return jdbc.getObject()
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM ai.prompt_templates WHERE status IN ('ACTIVE','DRAFT') ORDER BY "
                + FEATURE_ORDER
                + ", CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END")
        .query(JdbcPromptTemplateRepository::map)
        .list();
  }

  @Override
  public List<PromptTemplate> history(FeatureCode feature) {
    return jdbc.getObject()
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM ai.prompt_templates WHERE feature_code = :feature"
                + " ORDER BY created_at DESC, id")
        .param("feature", feature.name())
        .query(JdbcPromptTemplateRepository::map)
        .list();
  }

  @Override
  public Optional<PromptTemplate> find(FeatureCode feature, PromptStatus status) {
    return jdbc.getObject()
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM ai.prompt_templates WHERE feature_code = :feature AND status = :status")
        .param("feature", feature.name())
        .param("status", status.name())
        .query(JdbcPromptTemplateRepository::map)
        .optional();
  }

  @Override
  public void lock(FeatureCode feature) {
    jdbc.getObject()
        .sql("SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))")
        .param("key", "ai:prompt:" + feature.name())
        .query()
        .singleValue();
  }

  @Override
  public PromptTemplate createDraft(FeatureCode feature, PromptContent content, UUID actor) {
    return insert(feature, PromptStatus.DRAFT, content, actor);
  }

  @Override
  public PromptTemplate createActive(FeatureCode feature, PromptContent content, UUID actor) {
    return insert(feature, PromptStatus.ACTIVE, content, actor);
  }

  @Override
  public PromptTemplate updateDraft(UUID draftId, PromptContent content) {
    return jdbc.getObject()
        .sql(
            "UPDATE ai.prompt_templates SET system_instruction = :instruction, model_id = :model,"
                + " response_guards = CAST(:guards AS jsonb) WHERE id = :id RETURNING "
                + COLUMNS)
        .param("instruction", content.systemInstruction())
        .param("model", content.modelId())
        .param("guards", guardsJson(content.guards()))
        .param("id", draftId)
        .query(JdbcPromptTemplateRepository::map)
        .single();
  }

  @Override
  public void archive(UUID activeId) {
    jdbc.getObject()
        .sql("UPDATE ai.prompt_templates SET status = 'ARCHIVED' WHERE id = :id")
        .param("id", activeId)
        .update();
  }

  @Override
  public PromptTemplate promote(UUID draftId, PromptContent content) {
    return jdbc.getObject()
        .sql(
            "UPDATE ai.prompt_templates SET status = 'ACTIVE', published_at = now(),"
                + " system_instruction = :instruction, model_id = :model,"
                + " response_guards = CAST(:guards AS jsonb) WHERE id = :id RETURNING "
                + COLUMNS)
        .param("instruction", content.systemInstruction())
        .param("model", content.modelId())
        .param("guards", guardsJson(content.guards()))
        .param("id", draftId)
        .query(JdbcPromptTemplateRepository::map)
        .single();
  }

  private PromptTemplate insert(
      FeatureCode feature, PromptStatus status, PromptContent content, UUID actor) {
    // Schema version never changes through publish (BR-AI-13), so the new row copies the current
    // one.
    return jdbc.getObject()
        .sql(
            "INSERT INTO ai.prompt_templates (id, feature_code, version, status, system_instruction,"
                + " response_schema_version, model_id, response_guards, created_by, published_at)"
                + " VALUES (:id, :feature, :version, :status, :instruction,"
                + " (SELECT response_schema_version FROM ai.prompt_templates"
                + "   WHERE feature_code = :feature ORDER BY created_at DESC LIMIT 1),"
                + " :model, CAST(:guards AS jsonb), :actor,"
                + " CASE WHEN CAST(:status AS text) = 'ACTIVE' THEN now() END) RETURNING "
                + COLUMNS)
        .param("id", UUID.randomUUID())
        .param("feature", feature.name())
        .param("version", nextVersion(feature))
        .param("status", status.name())
        .param("instruction", content.systemInstruction())
        .param("model", content.modelId())
        .param("guards", guardsJson(content.guards()))
        .param("actor", actor)
        .query(JdbcPromptTemplateRepository::map)
        .single();
  }

  /** Largest {@code v<M>.<m>} of the feature with {@code m} plus one; v1.0 if none parses. */
  private String nextVersion(FeatureCode feature) {
    List<String> versions =
        jdbc.getObject()
            .sql("SELECT version FROM ai.prompt_templates WHERE feature_code = :feature")
            .param("feature", feature.name())
            .query(String.class)
            .list();
    long major = 1;
    long minor = -1;
    for (String version : versions) {
      Matcher m = VERSION.matcher(version);
      if (m.matches()) {
        long mj = Long.parseLong(m.group(1));
        long mn = Long.parseLong(m.group(2));
        if (mj > major || (mj == major && mn > minor)) {
          major = mj;
          minor = mn;
        }
      }
    }
    return "v" + major + "." + (minor + 1);
  }

  private static String guardsJson(ResponseGuards guards) {
    if (guards == null) {
      return null;
    }
    return JSON.writeValueAsString(guards);
  }

  private static PromptTemplate map(ResultSet rs, int row) throws SQLException {
    return new PromptTemplate(
        rs.getObject("id", UUID.class),
        FeatureCode.valueOf(rs.getString("feature_code")),
        rs.getString("version"),
        PromptStatus.valueOf(rs.getString("status")),
        new PromptContent(
            rs.getString("system_instruction"),
            rs.getString("model_id"),
            guards(rs.getString("guards"))),
        rs.getString("response_schema_version"),
        instant(rs, "published_at"),
        instant(rs, "created_at"));
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    var ts = rs.getTimestamp(column);
    return ts == null ? null : ts.toInstant();
  }

  private static ResponseGuards guards(String json) {
    if (json == null) {
      return null;
    }
    JsonNode node = JSON.readTree(json);
    return new ResponseGuards(
        node.path("citeLines").asBoolean(false),
        node.path("answerInVietnamese").asBoolean(false),
        node.path("noFullSolution").asBoolean(false));
  }
}
