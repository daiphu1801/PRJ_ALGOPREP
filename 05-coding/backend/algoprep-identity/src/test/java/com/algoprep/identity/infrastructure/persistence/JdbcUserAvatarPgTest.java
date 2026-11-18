package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/** Avatar metadata on PostgreSQL: strictly increasing versions, readable filter, constraints. */
class JdbcUserAvatarPgTest extends PostgresTestBase {
  private final JdbcUserAvatarRepository avatars = new JdbcUserAvatarRepository(provider());
  private final JdbcMyProfileRepository profile = new JdbcMyProfileRepository(provider());

  @Test
  void upsertInsertsThenReplacesWithAStrictlyLargerMillisecondVersion() {
    UUID id = user("av1@x.io", "STUDENT");

    AvatarRecord first = avatars.upsert(id, "users/a/1.png", "image/png", 10);
    long previous = first.version();
    for (int i = 0; i < 25; i++) {
      AvatarRecord next = avatars.upsert(id, "users/a/" + i + ".webp", "image/webp", 20 + i);
      assertThat(next.version()).isGreaterThan(previous);
      previous = next.version();
    }

    assertThat(count("user_avatars")).isEqualTo(1);
    assertThat(avatars.findForUpdate(id).orElseThrow().objectKey()).isEqualTo("users/a/24.webp");
    assertThat(profile.findById(id).orElseThrow().avatarVersion()).isEqualTo(previous);
  }

  @Test
  void aProfileWithoutAnAvatarHasANullVersion() {
    UUID id = user("av2@x.io", "STUDENT");

    assertThat(profile.findById(id).orElseThrow().avatarVersion()).isNull();
    assertThat(avatars.findForUpdate(id)).isEmpty();
  }

  @Test
  void readableHidesDeactivatedAndAnonymisedAccounts() {
    UUID active = user("av3@x.io", "STUDENT");
    UUID pending = user("av4@x.io", "STUDENT", "PENDING_VERIFICATION");
    UUID deactivated = user("av5@x.io", "STUDENT");
    UUID anonymised = user("av6@x.io", "STUDENT");
    for (UUID id : new UUID[] {active, pending, deactivated, anonymised}) {
      avatars.upsert(id, "users/" + id + "/a.png", "image/png", 5);
    }
    jdbc.sql(
            "UPDATE identity.users SET status='DEACTIVATED', deactivation_source='SELF',"
                + " deactivated_at=now() WHERE id=:id")
        .param("id", deactivated)
        .update();
    jdbc.sql(
            "UPDATE identity.users SET status='DEACTIVATED', deactivation_source='SELF',"
                + " deactivated_at=now() - interval '40 days', anonymized_at=now() WHERE id=:id")
        .param("id", anonymised)
        .update();

    assertThat(avatars.findReadable(active)).isPresent();
    assertThat(avatars.findReadable(pending)).isPresent();
    assertThat(avatars.findReadable(deactivated)).isEmpty();
    assertThat(avatars.findReadable(anonymised)).isEmpty();
    assertThat(avatars.findReadable(UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteReturnsTheRemovedRowOnceAndThenNothing() {
    UUID id = user("av7@x.io", "STUDENT");
    avatars.upsert(id, "users/a/z.jpg", "image/jpeg", 7);

    assertThat(avatars.delete(id).orElseThrow().objectKey()).isEqualTo("users/a/z.jpg");
    assertThat(avatars.delete(id)).isEmpty();
    assertThat(count("user_avatars")).isZero();
  }

  @Test
  void theTableRejectsAnUnknownTypeAnOversizedFileAndAnUnknownUser() {
    UUID id = user("av8@x.io", "STUDENT");

    assertThatThrownBy(() -> avatars.upsert(id, "k", "image/gif", 5))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() -> avatars.upsert(id, "k", "image/png", 524_289))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() -> avatars.upsert(id, "k", "image/png", 0))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() -> avatars.upsert(UUID.randomUUID(), "k", "image/png", 5))
        .isInstanceOf(DataAccessException.class);
    assertThat(avatars.upsert(id, "k", "image/png", 524_288).sizeBytes()).isEqualTo(524_288);
  }
}
