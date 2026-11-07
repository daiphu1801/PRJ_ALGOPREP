package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.ActivityEntry;
import com.algoprep.identity.domain.ports.out.RecentActivityRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcRecentActivityRepository implements RecentActivityRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcRecentActivityRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<UUID> classesEnrolledAt(UUID studentId, Instant at) {
    return jdbc.getObject()
        .sql(
            """
        SELECT class_id FROM identity.class_enrollments
        WHERE student_id = :student AND joined_at <= :at AND (left_at IS NULL OR left_at > :at)
        """)
        .param("student", studentId)
        .param("at", Timestamp.from(at))
        .query(UUID.class)
        .list();
  }

  @Override
  public void add(ActivityEntry e) {
    jdbc.getObject()
        .sql(
            """
        INSERT INTO identity.identity_recent_activity(
            id, actor_user_id, scope_class_id, activity_type, ref_type, ref_id, occurred_at)
        VALUES (:id, :actor, :class, :type, :refType, :refId, :at)
        """)
        .param("id", e.id())
        .param("actor", e.actorUserId())
        .param("class", e.classId())
        .param("type", e.activityType())
        .param("refType", e.refType())
        .param("refId", e.refId())
        .param("at", Timestamp.from(e.occurredAt()))
        .update();
  }

  @Override
  public boolean addIfClassExists(ActivityEntry e) {
    return jdbc.getObject()
            .sql(
                """
        INSERT INTO identity.identity_recent_activity(
            id, actor_user_id, scope_class_id, activity_type, ref_type, ref_id, occurred_at)
        SELECT :id, :actor, c.id, :type, :refType, :refId, :at
        FROM identity.classes c WHERE c.id = :class
        """)
            .param("id", e.id())
            .param("actor", e.actorUserId())
            .param("class", e.classId())
            .param("type", e.activityType())
            .param("refType", e.refType())
            .param("refId", e.refId())
            .param("at", Timestamp.from(e.occurredAt()))
            .update()
        > 0;
  }
}
