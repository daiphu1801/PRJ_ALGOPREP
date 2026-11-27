package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Avatar metadata (BD database section 1.16). Timestamps come from the database clock cut to the
 * millisecond, because {@code avatarVersion} is the epoch millisecond of {@code updated_at} and a
 * replace inside the same millisecond must still produce a larger value.
 */
@Repository
class JdbcUserAvatarRepository implements UserAvatarRepository {
  private static final String COLUMNS = "user_id, object_key, content_type, size_bytes, updated_at";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcUserAvatarRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<AvatarRecord> findForUpdate(UUID userId) {
    return jdbc.getObject()
        .sql("SELECT " + COLUMNS + " FROM identity.user_avatars WHERE user_id = :id FOR UPDATE")
        .param("id", userId)
        .query(JdbcUserAvatarRepository::map)
        .optional();
  }

  @Override
  public Optional<AvatarRecord> findReadable(UUID userId) {
    return jdbc.getObject()
        .sql(
            """
        SELECT a.user_id, a.object_key, a.content_type, a.size_bytes, a.updated_at
        FROM identity.user_avatars a JOIN identity.users u ON u.id = a.user_id
        WHERE a.user_id = :id AND u.status <> 'DEACTIVATED' AND u.anonymized_at IS NULL
        """)
        .param("id", userId)
        .query(JdbcUserAvatarRepository::map)
        .optional();
  }

  @Override
  public AvatarRecord upsert(UUID userId, String objectKey, String contentType, int sizeBytes) {
    return jdbc.getObject()
        .sql(
            """
        INSERT INTO identity.user_avatars AS a
          (user_id, object_key, content_type, size_bytes, updated_at)
        VALUES (:id, :key, :type, :size, date_trunc('milliseconds', clock_timestamp()))
        ON CONFLICT (user_id) DO UPDATE SET
          object_key = EXCLUDED.object_key,
          content_type = EXCLUDED.content_type,
          size_bytes = EXCLUDED.size_bytes,
          updated_at = greatest(
            date_trunc('milliseconds', clock_timestamp()),
            date_trunc('milliseconds', a.updated_at) + interval '1 millisecond')
        RETURNING a.user_id, a.object_key, a.content_type, a.size_bytes, a.updated_at
        """)
        .param("id", userId)
        .param("key", objectKey)
        .param("type", contentType)
        .param("size", sizeBytes)
        .query(JdbcUserAvatarRepository::map)
        .single();
  }

  @Override
  public Optional<AvatarRecord> delete(UUID userId) {
    return jdbc.getObject()
        .sql("DELETE FROM identity.user_avatars WHERE user_id = :id RETURNING " + COLUMNS)
        .param("id", userId)
        .query(JdbcUserAvatarRepository::map)
        .optional();
  }

  private static AvatarRecord map(ResultSet rs, int row) throws SQLException {
    return new AvatarRecord(
        rs.getObject("user_id", UUID.class),
        rs.getString("object_key"),
        rs.getString("content_type"),
        rs.getInt("size_bytes"),
        rs.getTimestamp("updated_at").toInstant());
  }
}
