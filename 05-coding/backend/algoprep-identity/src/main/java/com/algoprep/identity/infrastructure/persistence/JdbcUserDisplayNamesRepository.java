package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.ports.out.UserDisplayNamesRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcUserDisplayNamesRepository implements UserDisplayNamesRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcUserDisplayNamesRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Map<UUID, String> findDisplayNames(Set<UUID> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, String> result = new HashMap<>();
    jdbc.getObject()
        .sql(
            """
            SELECT id, CASE WHEN anonymized_at IS NOT NULL THEN 'Người dùng đã xoá'
              ELSE display_name END AS safe_name
            FROM identity.users WHERE id IN (:ids)
            """)
        .param("ids", userIds)
        .query(
            (rs, row) -> {
              result.put(rs.getObject("id", UUID.class), rs.getString("safe_name"));
              return rs.getObject("id", UUID.class);
            })
        .list();
    return Map.copyOf(result);
  }
}
