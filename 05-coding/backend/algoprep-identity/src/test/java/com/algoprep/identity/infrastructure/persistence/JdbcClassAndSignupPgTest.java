package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.model.ClassRef;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.model.OAuthProvider;
import com.algoprep.identity.domain.model.RegistrationAccount;
import com.algoprep.identity.domain.model.ScheduleSpec;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/** Class, invite code, enrollment, registration, bootstrap and OAuth persistence on PostgreSQL. */
class JdbcClassAndSignupPgTest extends PostgresTestBase {
  private final JdbcInstructorClassRepository classes =
      new JdbcInstructorClassRepository(provider());
  private final JdbcStudentClassRepository student = new JdbcStudentClassRepository(provider());
  private final JdbcRegistrationRepository registration =
      new JdbcRegistrationRepository(provider(), new JdbcUserAccountRepository(provider()));
  private final JdbcAdminBootstrapRepository bootstrap =
      new JdbcAdminBootstrapRepository(provider());
  private final JdbcOAuthAccountRepository oauth =
      new JdbcOAuthAccountRepository(provider(), UUID::randomUUID);

  private static final ScheduleSpec SCHEDULE =
      new ScheduleSpec(List.of(2, 4), "08:00", "10:00", "B1-02", "2026-01-05", null, null);

  private String status(UUID id) {
    return jdbc.sql("SELECT status FROM identity.users WHERE id = :id")
        .param("id", id)
        .query(String.class)
        .single();
  }

  // ---- classes -------------------------------------------------------------------------------

  @Test
  void createStoresScheduleAsJsonbAndReadsItBack() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID id = UUID.randomUUID();
    classes.create(id, teacher, "Algo", "desc", SCHEDULE);
    UUID bare = UUID.randomUUID();
    classes.create(bare, teacher, "Bare", null, null);

    assertThat(
            jdbc.sql("SELECT schedule::text FROM identity.classes WHERE id=:id")
                .param("id", id)
                .query(String.class)
                .single())
        .contains("\"room\": \"B1-02\"")
        .doesNotContain("endDate");
    InstructorClass read = classes.findOwned(id, teacher).orElseThrow();
    assertThat(read.schedule()).isEqualTo(SCHEDULE);
    assertThat(classes.findOwned(bare, teacher).orElseThrow().schedule()).isNull();
    assertThat(
            jdbc.sql("SELECT schedule IS NULL FROM identity.classes WHERE id=:id")
                .param("id", bare)
                .query(Boolean.class)
                .single())
        .isTrue();
    assertThat(classes.findOwned(id, UUID.randomUUID())).isEmpty();
    assertThat(classes.listOwned(teacher)).hasSize(2);
  }

  @Test
  void createWithUnknownInstructorViolatesForeignKey() {
    UUID stranger = UUID.randomUUID();
    UUID id = UUID.randomUUID();
    assertThatThrownBy(() -> classes.create(id, stranger, "X", null, null))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23503"));
    assertThat(count("classes")).isZero();
  }

  @Test
  void updateRewritesScheduleAndClearsItWhenAllPartsAreNull() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID id = UUID.randomUUID();
    classes.create(id, teacher, "Algo", "d", SCHEDULE);
    classes.update(
        id, "Renamed", "d2", new ScheduleSpec(List.of(3), "09:00", null, null, null, null, "note"));
    InstructorClass read = classes.findOwned(id, teacher).orElseThrow();
    assertThat(read.name()).isEqualTo("Renamed");
    assertThat(read.schedule().days()).containsExactly(3);
    assertThat(read.schedule().note()).isEqualTo("note");
    assertThat(read.schedule().room()).isNull();

    classes.update(id, "Renamed", "d2", null);
    assertThat(classes.findOwned(id, teacher).orElseThrow().schedule()).isNull();
  }

  @Test
  void inviteCodeConflictReturnsEmptyAndListsOnlyUnexpiredCodes() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID cls = classOf(teacher, "Algo");
    UUID other = classOf(teacher, "Other");
    assertThat(classes.insertInviteCode(UUID.randomUUID(), cls, teacher, "ABCD1234")).isPresent();
    assertThat(classes.insertInviteCode(UUID.randomUUID(), other, teacher, "ABCD1234")).isEmpty();
    assertThat(count("class_invite_codes")).isEqualTo(1);

    UUID expired = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO identity.class_invite_codes(id,class_id,code,expires_at,created_by)"
                + " VALUES (:id,:c,'OLD00000',now() - interval '1 day',:u)")
        .param("id", expired)
        .param("c", cls)
        .param("u", teacher)
        .update();
    assertThat(classes.listActiveInviteCodes(cls))
        .extracting(ClassInviteCode::code)
        .containsExactly("ABCD1234");
    assertThat(classes.listActiveInviteCodes(other)).isEmpty();
  }

  @Test
  void ownershipChecksAndDeleteCascadeToCodesAndEnrollments() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID rival = user("r@x.io", "INSTRUCTOR");
    UUID learner = user("s@x.io", "STUDENT");
    UUID cls = classOf(teacher, "Algo");
    classes.insertInviteCode(UUID.randomUUID(), cls, teacher, "CODE0001");
    student.joinByCode(learner, "CODE0001");
    assertThat(classes.isOwner(cls, teacher)).isTrue();
    assertThat(classes.isOwner(cls, rival)).isFalse();

    assertThat(classes.deleteOwned(cls, rival)).isFalse();
    assertThat(count("classes")).isEqualTo(1);
    assertThat(classes.deleteOwned(cls, teacher)).isTrue();
    assertThat(count("classes")).isZero();
    assertThat(count("class_invite_codes")).isZero();
    assertThat(count("class_enrollments")).isZero();
  }

  @Test
  void recentActivityKeepsRowButNullsScopeWhenClassIsDeleted() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID cls = classOf(teacher, "Algo");
    jdbc.sql(
            "INSERT INTO identity.identity_recent_activity(id,actor_user_id,scope_class_id,"
                + "activity_type,occurred_at) VALUES (:i,:u,:c,'CLASS_PROBLEM_PUBLISHED',now())")
        .param("i", UUID.randomUUID())
        .param("u", teacher)
        .param("c", cls)
        .update();
    classes.deleteOwned(cls, teacher);
    assertThat(count("identity_recent_activity")).isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT scope_class_id IS NULL FROM identity.identity_recent_activity")
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  // ---- enrollment ----------------------------------------------------------------------------

  @Test
  void joinByCodeEnrollsOnceReactivatesLeaversAndRejectsExpiredOrUnknownCodes() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID learner = user("s@x.io", "STUDENT");
    UUID cls = classOf(teacher, "Algo");
    classes.insertInviteCode(UUID.randomUUID(), cls, teacher, "CODE0001");

    assertThat(student.joinByCode(learner, "CODE0001")).map(ClassRef::name).contains("Algo");
    assertThat(student.joinByCode(learner, "CODE0001")).isPresent();
    assertThat(count("class_enrollments")).isEqualTo(1);
    assertThat(student.joinByCode(learner, "NOPE0000")).isEmpty();

    jdbc.sql("UPDATE identity.class_invite_codes SET expires_at = now() - interval '1 minute'")
        .update();
    assertThat(student.joinByCode(user("s2@x.io", "STUDENT"), "CODE0001")).isEmpty();
    assertThat(count("class_enrollments")).isEqualTo(1);

    // Leaving and joining again reuses the row and clears left_at.
    Instant left = classes.markLeft(cls, learner).orElseThrow();
    assertThat(left).isNotNull();
    assertThat(student.listActive(learner)).isEmpty();
    jdbc.sql("UPDATE identity.class_invite_codes SET expires_at = now() + interval '1 day'")
        .update();
    assertThat(student.joinByCode(learner, "CODE0001")).isPresent();
    assertThat(count("class_enrollments")).isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT left_at IS NULL FROM identity.class_enrollments")
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  @Test
  void duplicateEnrollmentRowViolatesUniqueConstraint() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID learner = user("s@x.io", "STUDENT");
    UUID cls = classOf(teacher, "Algo");
    String insert =
        "INSERT INTO identity.class_enrollments(id,class_id,student_id) VALUES (:i,:c,:s)";
    jdbc.sql(insert).param("i", UUID.randomUUID()).param("c", cls).param("s", learner).update();
    assertThatThrownBy(
            () ->
                jdbc.sql(insert)
                    .param("i", UUID.randomUUID())
                    .param("c", cls)
                    .param("s", learner)
                    .update())
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23505"));
    assertThat(count("class_enrollments")).isEqualTo(1);
  }

  @Test
  void studentViewsAndRosterShowOnlyActiveEnrollments() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID a = user("a@x.io", "STUDENT");
    UUID b = user("b@x.io", "STUDENT");
    UUID cls = classOf(teacher, "Algo");
    classes.insertInviteCode(UUID.randomUUID(), cls, teacher, "CODE0001");
    student.joinByCode(a, "CODE0001");
    student.joinByCode(b, "CODE0001");

    assertThat(classes.listActiveRoster(cls))
        .extracting(ClassRosterEntry::email)
        .containsExactlyInAnyOrder("a@x.io", "b@x.io");
    assertThat(classes.findOwned(cls, teacher).orElseThrow().studentCount()).isEqualTo(2);
    assertThat(student.findActive(a, cls).orElseThrow().instructorName())
        .isEqualTo("Name of t@x.io");
    assertThat(student.findActive(a, cls).orElseThrow().schedule()).isNull();

    assertThat(classes.markLeft(cls, a)).isPresent();
    // A second call returns the original leave time instead of failing.
    Instant first = classes.markLeft(cls, a).orElseThrow();
    assertThat(classes.markLeft(cls, a)).contains(first);
    assertThat(classes.markLeft(cls, UUID.randomUUID())).isEmpty();
    assertThat(classes.listActiveRoster(cls))
        .extracting(ClassRosterEntry::email)
        .containsExactly("b@x.io");
    assertThat(classes.findOwned(cls, teacher).orElseThrow().studentCount()).isEqualTo(1);
    assertThat(student.findActive(a, cls)).isEmpty();
    assertThat(student.listActive(b)).hasSize(1);
  }

  @Test
  void studentViewParsesScheduleJson() {
    UUID teacher = user("t@x.io", "INSTRUCTOR");
    UUID learner = user("s@x.io", "STUDENT");
    UUID cls = UUID.randomUUID();
    classes.create(cls, teacher, "Algo", null, SCHEDULE);
    classes.insertInviteCode(UUID.randomUUID(), cls, teacher, "CODE0001");
    student.joinByCode(learner, "CODE0001");
    var mine = student.findActive(learner, cls).orElseThrow();
    assertThat(mine.description()).isEmpty();
    assertThat(mine.schedule()).isInstanceOf(java.util.Map.class);
    assertThat(((java.util.Map<?, ?>) mine.schedule()).get("room")).isEqualTo("B1-02");
  }

  // ---- registration, bootstrap, oauth --------------------------------------------------------

  @Test
  void registrationCreateIsCaseInsensitiveUniqueAndActivateIsConditional() {
    UUID id = UUID.randomUUID();
    assertThat(registration.create(new RegistrationAccount(id, "New@X.io", "h", "New"))).isTrue();
    assertThat(
            registration.create(
                new RegistrationAccount(UUID.randomUUID(), "new@x.IO", "h2", "Dup")))
        .isFalse();
    assertThat(count("users")).isEqualTo(1);
    assertThat(status(id)).isEqualTo("PENDING_VERIFICATION");
    assertThat(
            jdbc.sql("SELECT r.code FROM identity.users u JOIN identity.roles r ON r.id=u.role_id")
                .query(String.class)
                .single())
        .isEqualTo("STUDENT");
    assertThat(registration.lockByEmail("NEW@x.io")).isPresent();
    assertThat(registration.lockByEmail("none@x.io")).isEmpty();

    registration.activate(id);
    assertThat(status(id)).isEqualTo("ACTIVE");

    // A forced-password-change account is not activated by e-mail verification.
    UUID forced = user("f@x.io", "STUDENT", "PENDING_VERIFICATION");
    jdbc.sql("UPDATE identity.users SET must_change_password = true WHERE id=:id")
        .param("id", forced)
        .update();
    registration.activate(forced);
    assertThat(status(forced)).isEqualTo("PENDING_VERIFICATION");
  }

  @Test
  void bootstrapCreatesAdminOnceByEmailAndReportsExistence() {
    assertThat(bootstrap.adminExists()).isFalse();
    UUID id = UUID.randomUUID();
    assertThat(bootstrap.createAdmin(id, "root@x.io", "h", "Root")).isTrue();
    assertThat(bootstrap.createAdmin(UUID.randomUUID(), "ROOT@x.io", "h", "Again")).isFalse();
    assertThat(bootstrap.adminExists()).isTrue();
    assertThat(count("users")).isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT must_change_password FROM identity.users WHERE id=:id")
                .param("id", id)
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  @Test
  void oauthLinkIsUniquePerProviderAccountAndCreateStudentLosesToExistingEmail() {
    UUID existing = user("s@x.io", "STUDENT");
    assertThat(oauth.link(existing, OAuthProvider.GITHUB, "gh-1", "s@x.io")).isTrue();
    UUID other = user("o@x.io", "STUDENT");
    assertThat(oauth.link(other, OAuthProvider.GITHUB, "gh-1", "o@x.io")).isFalse();
    assertThat(oauth.link(other, OAuthProvider.GOOGLE, "gh-1", "o@x.io")).isTrue();
    assertThat(count("oauth_identities")).isEqualTo(2);
    assertThat(oauth.findLinkedUser(OAuthProvider.GITHUB, "gh-1")).contains(existing);
    assertThat(oauth.findLinkedUser(OAuthProvider.GITHUB, "unknown")).isEmpty();

    assertThat(oauth.createStudent("S@X.io", "Dup")).isEmpty();
    UUID created = oauth.createStudent("new@x.io", "New").orElseThrow();
    assertThat(status(created)).isEqualTo("ACTIVE");
    assertThat(oauth.lockUserByEmail("NEW@x.io")).contains(created);
    assertThat(oauth.lockUserByEmail("zzz@x.io")).isEmpty();

    assertThatThrownBy(() -> oauth.link(UUID.randomUUID(), OAuthProvider.GITHUB, "gh-9", null))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23503"));
  }

  @Test
  void claimPendingAccountActivatesAndDropsPasswordOnlyForPlainPendingAccounts() {
    UUID pending = user("p@x.io", "STUDENT", "PENDING_VERIFICATION");
    jdbc.sql("UPDATE identity.users SET password_hash='secret' WHERE id=:id")
        .param("id", pending)
        .update();
    oauth.claimPendingAccount(pending);
    assertThat(status(pending)).isEqualTo("ACTIVE");
    assertThat(
            jdbc.sql("SELECT password_hash IS NULL FROM identity.users WHERE id=:id")
                .param("id", pending)
                .query(Boolean.class)
                .single())
        .isTrue();

    UUID forced = user("f@x.io", "STUDENT", "PENDING_VERIFICATION");
    jdbc.sql(
            "UPDATE identity.users SET must_change_password=true, password_hash='keep' WHERE id=:id")
        .param("id", forced)
        .update();
    oauth.claimPendingAccount(forced);
    assertThat(status(forced)).isEqualTo("PENDING_VERIFICATION");
  }

  @Test
  void emailUniqueIndexIsCaseInsensitiveAtTheDatabase() {
    user("same@x.io", "STUDENT");
    assertThatThrownBy(() -> user("SAME@x.io", "STUDENT"))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23505"));
    assertThat(count("users")).isEqualTo(1);
  }
}
