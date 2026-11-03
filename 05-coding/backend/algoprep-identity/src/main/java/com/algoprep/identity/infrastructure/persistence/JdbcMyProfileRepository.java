package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.model.ProfileFields;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Two reads in the caller's transaction: the account with its role, then the linked providers. */
@Repository
class JdbcMyProfileRepository implements MyProfileRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcMyProfileRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<MyProfile> findById(UUID userId) {
    JdbcClient client = jdbc.getObject();
    return client
        .sql(
            """
        SELECT u.display_name, u.email, u.school_or_company, u.current_position,
               u.target_position, u.default_language, u.phone, u.password_hash IS NOT NULL
                 AS has_password, u.password_changed_at, u.created_at, u.must_change_password,
               r.name AS role_name
        FROM identity.users u JOIN identity.roles r ON r.id = u.role_id
        WHERE u.id = :id
        """)
        .param("id", userId)
        .query(
            (rs, n) -> {
              Timestamp changed = rs.getTimestamp("password_changed_at");
              return new MyProfile(
                  userId,
                  rs.getString("display_name"),
                  rs.getString("email"),
                  rs.getString("school_or_company"),
                  rs.getString("current_position"),
                  rs.getString("target_position"),
                  rs.getString("default_language"),
                  rs.getString("phone"),
                  rs.getBoolean("has_password"),
                  changed == null ? null : changed.toInstant(),
                  providers(client, userId),
                  rs.getTimestamp("created_at").toInstant(),
                  rs.getString("role_name"),
                  rs.getBoolean("must_change_password"));
            })
        .optional();
  }

  @Override
  public void updateFields(UUID userId, ProfileFields fields) {
    jdbc.getObject()
        .sql(
            """
        UPDATE identity.users SET display_name = :name, school_or_company = :school,
          current_position = :current, target_position = :target, phone = :phone,
          updated_at = clock_timestamp()
        WHERE id = :id
        """)
        .param("name", fields.displayName())
        .param("school", fields.schoolOrCompany())
        .param("current", fields.currentPosition())
        .param("target", fields.targetPosition())
        .param("phone", fields.phone())
        .param("id", userId)
        .update();
  }

  @Override
  public boolean emailTaken(String email) {
    return jdbc.getObject()
        .sql("SELECT EXISTS (SELECT 1 FROM identity.users WHERE lower(email) = lower(:email))")
        .param("email", email)
        .query(Boolean.class)
        .single();
  }

  @Override
  public boolean updateEmail(UUID userId, String email) {
    try {
      jdbc.getObject()
          .sql(
              "UPDATE identity.users SET email = :email, updated_at = clock_timestamp()"
                  + " WHERE id = :id")
          .param("email", email)
          .param("id", userId)
          .update();
      return true;
    } catch (DuplicateKeyException e) {
      return false;
    }
  }

  private static List<String> providers(JdbcClient client, UUID userId) {
    return client
        .sql(
            "SELECT provider FROM identity.oauth_identities WHERE user_id = :id"
                + " ORDER BY linked_at, provider")
        .param("id", userId)
        .query(String.class)
        .list();
  }
}
