package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.ports.out.AdminBootstrapRepository;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** JDBC adapter for {@link AdminBootstrapRepository}; relies on the lower(email) unique index. */
@Repository
class JdbcAdminBootstrapRepository implements AdminBootstrapRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAdminBootstrapRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean adminExists() {
    return jdbc.getObject()
        .sql(
            "SELECT EXISTS (SELECT 1 FROM identity.users u JOIN identity.roles r"
                + " ON r.id = u.role_id WHERE r.base_category = 'ADMIN')")
        .query(Boolean.class)
        .single();
  }

  @Override
  public boolean createAdmin(UUID id, String email, String passwordHash, String displayName) {
    return jdbc.getObject()
            .sql(
                """
        INSERT INTO identity.users
          (id, email, password_hash, display_name, role_id, status, must_change_password)
        SELECT :id, :email, :hash, :name, id, 'PENDING_VERIFICATION', true
        FROM identity.roles WHERE code = 'ADMIN' AND is_system = true
        ON CONFLICT (lower(email)) DO NOTHING
        """)
            .param("id", id)
            .param("email", email)
            .param("hash", passwordHash)
            .param("name", displayName)
            .update()
        == 1;
  }
}
