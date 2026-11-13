package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.ports.out.UserBaseCategoriesRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcUserBaseCategoriesRepository implements UserBaseCategoriesRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcUserBaseCategoriesRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Map<UUID, BaseCategory> findBaseCategories(Set<UUID> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    Map<UUID, BaseCategory> result = new HashMap<>();
    jdbc.getObject()
        .sql(
            """
            SELECT u.id, r.base_category FROM identity.users u
            JOIN identity.roles r ON r.id = u.role_id WHERE u.id IN (:ids)
            """)
        .param("ids", userIds)
        .query(
            (rs, row) -> {
              result.put(
                  rs.getObject("id", UUID.class),
                  BaseCategory.valueOf(rs.getString("base_category")));
              return row;
            })
        .list();
    return Map.copyOf(result);
  }
}
