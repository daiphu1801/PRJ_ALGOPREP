package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.MySettings;
import com.algoprep.identity.domain.ports.out.MySettingsRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Defaults in the read mirror the column defaults of {@code user_preferences}. */
@Repository
class JdbcMySettingsRepository implements MySettingsRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcMySettingsRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<MySettings> find(UUID userId) {
    return read(userId);
  }

  @Override
  public Optional<MySettings> findForUpdate(UUID userId) {
    // Lock in its own statement: a locking join would keep the preferences row it saw before
    // the wait, so the values are read again once the lock is held.
    int locked =
        jdbc.getObject()
            .sql("SELECT 1 FROM identity.users WHERE id = :id FOR UPDATE")
            .param("id", userId)
            .query(Integer.class)
            .list()
            .size();
    return locked == 0 ? Optional.empty() : read(userId);
  }

  private Optional<MySettings> read(UUID userId) {
    return jdbc.getObject()
        .sql(
            """
        SELECT COALESCE(p.streak_reminder_enabled, true) AS streak,
               COALESCE(p.weekly_report_enabled, false) AS weekly,
               COALESCE(p.preferred_interview_level, 'MIDDLE') AS level,
               COALESCE(p.preferred_max_turns, 12) AS turns,
               COALESCE(p.preferred_hint_allowed, true) AS hint,
               u.default_language AS language
        FROM identity.users u LEFT JOIN identity.user_preferences p ON p.user_id = u.id
        WHERE u.id = :id
        """)
        .param("id", userId)
        .query(
            (rs, n) ->
                new MySettings(
                    rs.getBoolean("streak"),
                    rs.getBoolean("weekly"),
                    rs.getString("level"),
                    rs.getInt("turns"),
                    rs.getBoolean("hint"),
                    rs.getString("language")))
        .optional();
  }

  @Override
  public void save(UUID userId, MySettings s) {
    JdbcClient client = jdbc.getObject();
    client
        .sql(
            """
        INSERT INTO identity.user_preferences
          (user_id, streak_reminder_enabled, weekly_report_enabled, preferred_interview_level,
           preferred_max_turns, preferred_hint_allowed)
        VALUES (:id, :streak, :weekly, :level, :turns, :hint)
        ON CONFLICT (user_id) DO UPDATE SET
          streak_reminder_enabled = EXCLUDED.streak_reminder_enabled,
          weekly_report_enabled = EXCLUDED.weekly_report_enabled,
          preferred_interview_level = EXCLUDED.preferred_interview_level,
          preferred_max_turns = EXCLUDED.preferred_max_turns,
          preferred_hint_allowed = EXCLUDED.preferred_hint_allowed,
          updated_at = clock_timestamp()
        """)
        .param("id", userId)
        .param("streak", s.streakReminderEnabled())
        .param("weekly", s.weeklyReportEnabled())
        .param("level", s.preferredInterviewLevel())
        .param("turns", s.preferredMaxTurns())
        .param("hint", s.preferredHintAllowed())
        .update();
    client
        .sql(
            "UPDATE identity.users SET default_language = :language,"
                + " updated_at = clock_timestamp() WHERE id = :id")
        .param("language", s.defaultLanguage())
        .param("id", userId)
        .update();
  }
}
