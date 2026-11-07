package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.ClassActivityItem;
import com.algoprep.identity.domain.ports.out.ClassActivityReadRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Joins the feed to the instructor's own classes and to the display name of the actor. */
@Repository
class JdbcClassActivityReadRepository implements ClassActivityReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcClassActivityReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<ClassActivityItem> recent(UUID instructorId, UUID classId, Instant since, int limit) {
    return jdbc.getObject()
        .sql(
            """
            SELECT a.id, a.activity_type, a.actor_user_id, u.display_name, a.scope_class_id,
                   a.ref_type, a.ref_id, a.occurred_at
            FROM identity.identity_recent_activity a
            JOIN identity.classes c ON c.id = a.scope_class_id AND c.instructor_id = :instructor
            JOIN identity.users u ON u.id = a.actor_user_id
            WHERE a.occurred_at >= :since
              AND (CAST(:classId AS uuid) IS NULL OR a.scope_class_id = CAST(:classId AS uuid))
            ORDER BY a.occurred_at DESC, a.id DESC LIMIT :limit
            """)
        .param("instructor", instructorId)
        .param("classId", classId)
        .param("since", Timestamp.from(since))
        .param("limit", limit)
        .query(
            (rs, n) ->
                new ClassActivityItem(
                    rs.getObject("id", UUID.class),
                    rs.getString("activity_type"),
                    rs.getObject("actor_user_id", UUID.class),
                    rs.getString("display_name"),
                    rs.getObject("scope_class_id", UUID.class),
                    rs.getString("ref_type"),
                    rs.getObject("ref_id", UUID.class),
                    rs.getTimestamp("occurred_at").toInstant()))
        .list();
  }
}
