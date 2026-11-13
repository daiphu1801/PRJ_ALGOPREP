package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Writes {@code identity.system_audit_logs} rows in the caller's transaction (DD BR-24). */
@Repository
class JdbcAuditLogAdapter implements AuditLogPort {

  private final ObjectProvider<JdbcClient> jdbcProvider;
  private final IdGeneratorPort ids;
  private final ClockPort clock;

  JdbcAuditLogAdapter(
      ObjectProvider<JdbcClient> jdbcProvider, IdGeneratorPort ids, ClockPort clock) {
    this.jdbcProvider = jdbcProvider;
    this.ids = ids;
    this.clock = clock;
  }

  @Override
  public void record(AuditEntry entry) {
    jdbcProvider
        .getObject()
        .sql(
            "INSERT INTO identity.system_audit_logs"
                + " (id, actor_user_id, action_type, target_type, target_id, before_json,"
                + " after_json, created_at) VALUES (:id, :actor, :action, :targetType, :targetId,"
                + " CAST(:before AS jsonb), CAST(:after AS jsonb), :at)")
        .param("id", ids.newId())
        .param("actor", entry.actorUserId())
        .param("action", entry.actionType())
        .param("targetType", entry.targetType())
        .param("targetId", entry.targetId(), Types.VARCHAR)
        .param("before", entry.beforeJson(), Types.VARCHAR)
        .param("after", entry.afterJson(), Types.VARCHAR)
        .param("at", Timestamp.from(clock.now()))
        .update();
  }
}
