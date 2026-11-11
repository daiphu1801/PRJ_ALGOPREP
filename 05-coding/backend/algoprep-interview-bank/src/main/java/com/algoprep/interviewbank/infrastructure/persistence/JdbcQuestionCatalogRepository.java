package com.algoprep.interviewbank.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.CatalogSlug;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL identifiers come exclusively from CatalogKind, never from client input. */
@Repository
class JdbcQuestionCatalogRepository implements CatalogRepository {
  private static final int MAX_SLUG_ATTEMPTS = 20;
  private static final long TOPIC_LOCK = 2601001L;
  private static final long LEVEL_LOCK = 2601002L;

  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;

  JdbcQuestionCatalogRepository(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  private static String table(CatalogKind kind) {
    return kind == CatalogKind.LEVEL
        ? "interview_bank.question_levels"
        : "interview_bank.question_topics";
  }

  private static String star(CatalogKind kind) {
    return kind == CatalogKind.TOPIC ? "c.uses_star_framework" : "false";
  }

  private static String referenceColumn(CatalogKind kind) {
    return kind == CatalogKind.LEVEL ? "level_id" : "topic_id";
  }

  private static ResourceConflictException duplicate(CatalogKind kind) {
    return new ResourceConflictException(
        kind == CatalogKind.LEVEL ? "INB-021" : "INB-020", "Catalogue name already exists");
  }

  private String select(CatalogKind kind, boolean counts) {
    String count =
        counts
            ? "(SELECT count(*) FROM interview_bank.interview_questions q WHERE q."
                + referenceColumn(kind)
                + " = c.id)"
            : "NULL::bigint";
    return "SELECT c.id, c.code, c.display_name, c.sort_order, "
        + star(kind)
        + " AS star, "
        + count
        + " AS question_count FROM "
        + table(kind)
        + " c";
  }

  private static CatalogItem map(ResultSet rs) throws SQLException {
    return new CatalogItem(
        rs.getObject("id", UUID.class),
        rs.getString("code"),
        rs.getString("display_name"),
        rs.getInt("sort_order"),
        rs.getBoolean("star"),
        rs.getObject("question_count", Long.class));
  }

  @Override
  public List<CatalogItem> list(CatalogKind kind, boolean counts) {
    return jdbc.getObject()
        .sql(select(kind, counts) + " ORDER BY c.sort_order, c.code")
        .query((rs, row) -> map(rs))
        .list();
  }

  @Override
  public void lock(CatalogKind kind) {
    // The advisory lock also covers an empty catalogue; FOR UPDATE blocks concurrent FK inserts.
    jdbc.getObject()
        .sql("SELECT pg_advisory_xact_lock(:key)")
        .param("key", kind == CatalogKind.LEVEL ? LEVEL_LOCK : TOPIC_LOCK)
        .query((rs, row) -> 1)
        .single();
    jdbc.getObject()
        .sql("SELECT id FROM " + table(kind) + " ORDER BY id FOR UPDATE")
        .query(UUID.class)
        .list();
  }

  @Override
  public Optional<CatalogItem> find(CatalogKind kind, UUID id) {
    return jdbc.getObject()
        .sql(select(kind, false) + " WHERE c.id = :id")
        .param("id", id)
        .query((rs, row) -> map(rs))
        .optional();
  }

  @Override
  public boolean nameTaken(CatalogKind kind, String displayName, UUID excluded) {
    return jdbc.getObject()
        .sql(
            "SELECT EXISTS (SELECT 1 FROM "
                + table(kind)
                + " c WHERE lower(c.display_name) = lower(:name)"
                + " AND (CAST(:excluded AS uuid) IS NULL OR c.id <> CAST(:excluded AS uuid)))")
        .param("name", displayName)
        .param("excluded", excluded, java.sql.Types.OTHER)
        .query(Boolean.class)
        .single();
  }

  @Override
  public CatalogItem create(CatalogKind kind, String displayName) {
    int order =
        jdbc.getObject()
            .sql("SELECT coalesce(max(sort_order), 0) + 1 FROM " + table(kind))
            .query(Integer.class)
            .single();
    String starColumn = kind == CatalogKind.TOPIC ? ", uses_star_framework" : "";
    String starValue = kind == CatalogKind.TOPIC ? ", false" : "";
    for (int attempt = 1; attempt <= MAX_SLUG_ATTEMPTS; attempt++) {
      String code = CatalogSlug.create(displayName, kind, attempt);
      UUID id = ids.newId();
      try {
        int inserted =
            jdbc.getObject()
                .sql(
                    "INSERT INTO "
                        + table(kind)
                        + " (id, code, display_name, sort_order"
                        + starColumn
                        + ") VALUES (:id, :code, :name, :order"
                        + starValue
                        + ") ON CONFLICT (code) DO NOTHING")
                .param("id", id)
                .param("code", code)
                .param("name", displayName)
                .param("order", order)
                .update();
        if (inserted == 1) {
          return new CatalogItem(id, code, displayName, order, false, 0L);
        }
      } catch (DuplicateKeyException conflict) {
        throw duplicate(kind);
      }
    }
    throw new IllegalStateException("Catalogue slug collision limit exceeded");
  }

  @Override
  public void update(CatalogKind kind, UUID id, String displayName, Boolean usesStarFramework) {
    boolean topic = kind == CatalogKind.TOPIC;
    var statement =
        jdbc.getObject()
            .sql(
                "UPDATE "
                    + table(kind)
                    + " SET display_name = coalesce(:name, display_name)"
                    + (topic ? ", uses_star_framework = coalesce(:star, uses_star_framework)" : "")
                    + " WHERE id = :id")
            .param("name", displayName)
            .param("id", id);
    if (topic) {
      statement = statement.param("star", usesStarFramework);
    }
    try {
      statement.update();
    } catch (DuplicateKeyException conflict) {
      throw duplicate(kind);
    }
  }

  @Override
  public void reorder(CatalogKind kind, List<UUID> orderedIds) {
    for (int index = 0; index < orderedIds.size(); index++) {
      jdbc.getObject()
          .sql("UPDATE " + table(kind) + " SET sort_order = :order WHERE id = :id")
          .param("order", index + 1)
          .param("id", orderedIds.get(index))
          .update();
    }
  }

  @Override
  public long references(CatalogKind kind, UUID id) {
    return jdbc.getObject()
        .sql(
            "SELECT count(*) FROM interview_bank.interview_questions WHERE "
                + referenceColumn(kind)
                + " = :id")
        .param("id", id)
        .query(Long.class)
        .single();
  }

  @Override
  public void delete(CatalogKind kind, UUID id) {
    jdbc.getObject().sql("DELETE FROM " + table(kind) + " WHERE id = :id").param("id", id).update();
  }
}
