package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProcessedEventRepository implements ProcessedEventRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcProcessedEventRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean markProcessed(UUID eventId, String eventType) {
    return jdbc.getObject()
            .sql(
                """
        INSERT INTO identity.processed_domain_events(event_id, event_type)
        VALUES (:id, :type) ON CONFLICT (event_id) DO NOTHING
        """)
            .param("id", eventId)
            .param("type", eventType)
            .update()
        == 1;
  }
}
