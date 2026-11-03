package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.problembank.domain.model.CatalogItem;
import com.algoprep.problembank.domain.model.CatalogKind;
import com.algoprep.problembank.domain.model.CatalogSlug;
import com.algoprep.problembank.domain.ports.out.CatalogRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL identifiers come exclusively from CatalogKind, never client input. */
@Repository
class JdbcCatalogRepository implements CatalogRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;

  JdbcCatalogRepository(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  private String table(CatalogKind kind) {
    return kind == CatalogKind.LEVEL ? "problem.problem_levels" : "problem.topics";
  }

  private String column(CatalogKind kind) {
    return kind == CatalogKind.LEVEL ? "display_name" : "name";
  }

  private String duplicate(CatalogKind kind) {
    return kind == CatalogKind.LEVEL ? "PRB-022" : "PRB-021";
  }

  @Override
  public List<CatalogItem> list(CatalogKind kind, boolean counts) {
    String count =
        !counts
            ? "NULL::bigint"
            : kind == CatalogKind.LEVEL
                ? "(SELECT count(*) FROM problem.problems p WHERE p.level_id = c.id)"
                : "(SELECT count(*) FROM problem.problem_topics p WHERE p.topic_id = c.id)";
    return jdbc.getObject()
        .sql(
            "SELECT c.id, c.code, c."
                + column(kind)
                + " AS name, c.sort_order, "
                + count
                + " AS problem_count FROM "
                + table(kind)
                + " c ORDER BY c.sort_order, c.code")
        .query(
            (rs, row) ->
                new CatalogItem(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getInt("sort_order"),
                    rs.getObject("problem_count", Long.class)))
        .list();
  }

  @Override
  public void lock(CatalogKind kind) {
    // The transaction lock also covers an empty topic catalogue and concurrent creates/reorders.
    jdbc.getObject()
        .sql("SELECT pg_advisory_xact_lock(:key)")
        .param("key", kind == CatalogKind.LEVEL ? 2201001L : 2201002L)
        .query((rs, row) -> 1)
        .single();
    jdbc.getObject()
        .sql("SELECT id FROM " + table(kind) + " ORDER BY id FOR UPDATE")
        .query(UUID.class)
        .list();
  }

  @Override
  public CatalogItem create(CatalogKind kind, String name) {
    int order = list(kind, false).stream().mapToInt(CatalogItem::sortOrder).max().orElse(0) + 1;
    for (int attempt = 1; attempt <= 20; attempt++) {
      String code = CatalogSlug.create(name, kind, attempt);
      UUID id = ids.newId();
      try {
        int inserted =
            jdbc.getObject()
                .sql(
                    "INSERT INTO "
                        + table(kind)
                        + " (id, code, "
                        + column(kind)
                        + ", sort_order) VALUES (:id, :code, :name, :order) ON CONFLICT (code) DO NOTHING")
                .param("id", id)
                .param("code", code)
                .param("name", name)
                .param("order", order)
                .update();
        if (inserted == 1) {
          return new CatalogItem(id, code, name, order, null);
        }
      } catch (DuplicateKeyException conflict) {
        throw new ResourceConflictException(duplicate(kind), "Catalogue name already exists");
      }
    }
    throw new IllegalStateException("Catalogue slug collision limit exceeded");
  }

  @Override
  public void rename(CatalogKind kind, UUID id, String name) {
    try {
      jdbc.getObject()
          .sql("UPDATE " + table(kind) + " SET " + column(kind) + " = :name WHERE id = :id")
          .param("name", name)
          .param("id", id)
          .update();
    } catch (DuplicateKeyException conflict) {
      throw new ResourceConflictException(duplicate(kind), "Catalogue name already exists");
    }
  }

  @Override
  public void reorder(CatalogKind kind, List<UUID> ordered) {
    for (int index = 0; index < ordered.size(); index++) {
      jdbc.getObject()
          .sql("UPDATE " + table(kind) + " SET sort_order = :order WHERE id = :id")
          .param("order", index + 1)
          .param("id", ordered.get(index))
          .update();
    }
  }

  @Override
  public long references(CatalogKind kind, UUID id) {
    String sql =
        kind == CatalogKind.LEVEL
            ? "SELECT count(*) FROM problem.problems WHERE level_id = :id"
            : "SELECT count(*) FROM problem.problem_topics WHERE topic_id = :id";
    return jdbc.getObject().sql(sql).param("id", id).query(Long.class).single();
  }

  @Override
  public void delete(CatalogKind kind, UUID id) {
    jdbc.getObject().sql("DELETE FROM " + table(kind) + " WHERE id = :id").param("id", id).update();
  }
}
