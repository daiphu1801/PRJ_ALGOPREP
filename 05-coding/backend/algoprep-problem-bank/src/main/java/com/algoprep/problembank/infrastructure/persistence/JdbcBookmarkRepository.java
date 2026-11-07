package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.StudentCatalogModels.Bookmark;
import com.algoprep.problembank.domain.model.StudentCatalogModels.BookmarkSaved;
import com.algoprep.problembank.domain.ports.out.BookmarkRepository;
import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcBookmarkRepository implements BookmarkRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcBookmarkRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<Bookmark> list(UUID userId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT p.id, p.code, p.title, pl.code AS level_code, pl.display_name,
                   b.note, b.created_at,
                   (p.status <> 'PUBLISHED' OR p.deleted) AS withdrawn,
                   ARRAY(SELECT t.name FROM problem.problem_topics pt
                         JOIN problem.topics t ON t.id = pt.topic_id
                         WHERE pt.problem_id = p.id ORDER BY t.sort_order, t.name) AS topic_names
            FROM problem.bookmarks b
            JOIN problem.problems p ON p.id = b.problem_id
            JOIN problem.problem_levels pl ON pl.id = p.level_id
            WHERE b.user_id = :user
            ORDER BY b.created_at DESC, p.id
            """)
        .param("user", userId)
        .query(
            (rs, row) ->
                new Bookmark(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getString("level_code"),
                    rs.getString("display_name"),
                    names(rs.getArray("topic_names")),
                    rs.getString("note"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getBoolean("withdrawn")))
        .list();
  }

  @Override
  public boolean levelCodeExists(String code) {
    return jdbc.getObject()
        .sql("SELECT EXISTS (SELECT 1 FROM problem.problem_levels WHERE code = :code)")
        .param("code", code)
        .query(Boolean.class)
        .single();
  }

  @Override
  public Optional<BookmarkSaved> updateNote(UUID userId, UUID problemId, String note) {
    return jdbc.getObject()
        .sql(
            """
            UPDATE problem.bookmarks SET note = :note, updated_at = now()
            WHERE user_id = :user AND problem_id = :problem
            RETURNING note, created_at
            """)
        .param("note", note)
        .param("user", userId)
        .param("problem", problemId)
        .query(
            (rs, row) ->
                new BookmarkSaved(
                    rs.getString("note"), rs.getTimestamp("created_at").toInstant(), false))
        .optional();
  }

  @Override
  public BookmarkSaved insert(UUID userId, UUID problemId, String note) {
    return jdbc.getObject()
        .sql(
            """
            INSERT INTO problem.bookmarks(user_id, problem_id, note) VALUES (:user, :problem, :note)
            ON CONFLICT (user_id, problem_id) DO UPDATE SET note = EXCLUDED.note, updated_at = now()
            RETURNING note, created_at, (xmax = 0) AS created
            """)
        .param("user", userId)
        .param("problem", problemId)
        .param("note", note)
        .query(
            (rs, row) ->
                new BookmarkSaved(
                    rs.getString("note"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getBoolean("created")))
        .single();
  }

  @Override
  public boolean problemPublished(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT EXISTS (SELECT 1 FROM problem.problems
                           WHERE id = :id AND status = 'PUBLISHED' AND NOT deleted)
            """)
        .param("id", problemId)
        .query(Boolean.class)
        .single();
  }

  @Override
  public boolean exists(UUID userId, UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT EXISTS (SELECT 1 FROM problem.bookmarks
                           WHERE user_id = :user AND problem_id = :problem)
            """)
        .param("user", userId)
        .param("problem", problemId)
        .query(Boolean.class)
        .single();
  }

  @Override
  public void delete(UUID userId, UUID problemId) {
    jdbc.getObject()
        .sql("DELETE FROM problem.bookmarks WHERE user_id = :user AND problem_id = :problem")
        .param("user", userId)
        .param("problem", problemId)
        .update();
  }

  private static List<String> names(Array array) throws SQLException {
    var names = new ArrayList<String>();
    for (Object name : (Object[]) array.getArray()) {
      names.add((String) name);
    }
    return names;
  }
}
