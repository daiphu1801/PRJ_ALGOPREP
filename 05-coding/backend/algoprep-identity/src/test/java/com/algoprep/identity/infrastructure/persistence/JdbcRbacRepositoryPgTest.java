package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.CatalogEntry;
import com.algoprep.identity.domain.model.RbacRole;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/** RBAC persistence against real PostgreSQL: seed matrix, upsert, version bump, locks, guards. */
class JdbcRbacRepositoryPgTest extends PostgresTestBase {
  private final UUID generated = UUID.randomUUID();
  private final JdbcRbacRepository repo = new JdbcRbacRepository(provider(), () -> generated);

  private boolean granted(String role, String function, String action) {
    return jdbc.sql(
            "SELECT granted FROM identity.permissions WHERE role_id = :r AND function_id = :f"
                + " AND action_id = :a")
        .param("r", roleId(role))
        .param("f", functionId(function))
        .param("a", actionId(action))
        .query(Boolean.class)
        .single();
  }

  private long version(String role) {
    return jdbc.sql("SELECT permission_version FROM identity.roles WHERE code = :c")
        .param("c", role)
        .query(Long.class)
        .single();
  }

  private void userInRole(UUID role, String email, String status) {
    jdbc.sql(
            "INSERT INTO identity.users(id,email,display_name,role_id,status)"
                + " VALUES(:id,:e,'N',:r,:s)")
        .param("id", UUID.randomUUID())
        .param("e", email)
        .param("r", role)
        .param("s", status)
        .update();
  }

  @Test
  void seedGivesThreeSystemRolesAndFullMatrixInOrder() {
    var roles = repo.roles();
    assertThat(roles)
        .extracting(RbacRole::code)
        .containsExactlyInAnyOrder("ADMIN", "INSTRUCTOR", "STUDENT");
    assertThat(roles).allMatch(RbacRole::isSystem);
    assertThat(repo.functions()).hasSize(11);
    assertThat(repo.actions())
        .extracting(CatalogEntry::code)
        .containsExactly("CREATE", "READ", "UPDATE", "DELETE");
    assertThat(repo.cells()).hasSize(132);
    assertThat(count("permissions")).isEqualTo(132);
    assertThat(repo.listRoles()).allMatch(r -> r.userCount() == 0);
  }

  @Test
  void migrationV1004GrantsInstructorInterviewBankAndBumpsVersionOnce() {
    for (String action : new String[] {"CREATE", "READ", "UPDATE", "DELETE"}) {
      assertThat(granted("INSTRUCTOR", "INTERVIEW_BANK_MANAGEMENT", action)).isTrue();
    }
    assertThat(granted("INSTRUCTOR", "USER_MANAGEMENT", "READ")).isFalse();
    assertThat(version("INSTRUCTOR")).isEqualTo(1);
    assertThat(version("ADMIN")).isZero();
  }

  @Test
  void setGrantedUpsertsOnConflictWithoutAddingRows() {
    UUID actor = user("a@x.io", "ADMIN");
    UUID role = roleId("STUDENT");
    UUID fn = functionId("CLASS_MANAGEMENT");
    UUID act = actionId("READ");
    assertThat(repo.isGranted(role, fn, act)).isFalse();

    repo.setGranted(role, fn, act, true, actor);
    assertThat(repo.isGranted(role, fn, act)).isTrue();
    assertThat(count("permissions")).isEqualTo(132);
    assertThat(
            jdbc.sql(
                    "SELECT updated_by FROM identity.permissions WHERE role_id=:r"
                        + " AND function_id=:f AND action_id=:a")
                .param("r", role)
                .param("f", fn)
                .param("a", act)
                .query(UUID.class)
                .single())
        .isEqualTo(actor);

    repo.setGranted(role, fn, act, false, actor);
    assertThat(repo.isGranted(role, fn, act)).isFalse();
    assertThat(count("permissions")).isEqualTo(132);
  }

  @Test
  void setGrantedInsertsMissingCellForCustomRole() {
    UUID actor = user("a@x.io", "ADMIN");
    var role = new RbacRole(UUID.randomUUID(), "TA", "Teaching assistant", "INSTRUCTOR", false);
    assertThat(repo.insertRole(role)).isTrue();
    UUID fn = functionId("CLASS_MANAGEMENT");
    UUID act = actionId("UPDATE");
    assertThat(repo.isGranted(role.id(), fn, act)).isFalse();
    assertThat(repo.cells().stream().filter(c -> c.roleId().equals(role.id())).count())
        .isEqualTo(44);

    repo.setGranted(role.id(), fn, act, true, actor);

    assertThat(repo.isGranted(role.id(), fn, act)).isTrue();
    assertThat(count("permissions")).isEqualTo(133);
  }

  @Test
  void setGrantedWithUnknownActorViolatesForeignKey() {
    UUID role = roleId("STUDENT");
    UUID fn = functionId("CLASS_MANAGEMENT");
    UUID act = actionId("READ");
    UUID stranger = UUID.randomUUID();
    assertThatThrownBy(() -> repo.setGranted(role, fn, act, true, stranger))
        .isInstanceOfSatisfying(
            DataAccessException.class, e -> assertThat(sqlState(e)).isEqualTo("23503"));
    assertThat(granted("STUDENT", "CLASS_MANAGEMENT", "READ")).isFalse();
  }

  @Test
  void bumpPermissionVersionIncrementsOnlyThatRole() {
    long student = version("STUDENT");
    long admin = version("ADMIN");
    repo.bumpPermissionVersion(roleId("ADMIN"));
    repo.bumpPermissionVersion(roleId("ADMIN"));
    assertThat(version("ADMIN")).isEqualTo(admin + 2);
    assertThat(version("STUDENT")).isEqualTo(student);
  }

  @Test
  void matrixEditorGuardCountsOnlyActiveAdminCategoryUsersOutsideTheRole() {
    UUID adminRole = roleId("ADMIN");
    assertThat(repo.isMatrixEditorRole(adminRole)).isTrue();
    assertThat(repo.isMatrixEditorRole(roleId("INSTRUCTOR"))).isFalse();
    assertThat(repo.isMatrixEditorRole(UUID.randomUUID())).isFalse();

    UUID actor = user("admin1@x.io", "ADMIN");
    user("admin2@x.io", "ADMIN", "DEACTIVATED");
    user("teacher@x.io", "INSTRUCTOR");
    // The only editor role is ADMIN, so nobody sits outside it.
    assertThat(repo.activeMatrixEditorsOutsideRole(adminRole)).isZero();

    // A second ADMIN-category role that can edit the matrix, one active and one inactive user.
    UUID second = UUID.randomUUID();
    repo.insertRole(new RbacRole(second, "SUPER", "Super", "ADMIN", false));
    repo.setGranted(second, functionId("PERMISSION_MATRIX"), actionId("UPDATE"), true, actor);
    userInRole(second, "s1@x.io", "ACTIVE");
    userInRole(second, "s2@x.io", "DEACTIVATED");

    assertThat(repo.isMatrixEditorRole(second)).isTrue();
    assertThat(repo.activeMatrixEditorsOutsideRole(adminRole)).isEqualTo(1);
    assertThat(repo.activeMatrixEditorsOutsideRole(second)).isEqualTo(1);

    // Revoking the grant removes the role from the guard.
    repo.setGranted(second, functionId("PERMISSION_MATRIX"), actionId("UPDATE"), false, actor);
    assertThat(repo.isMatrixEditorRole(second)).isFalse();
    assertThat(repo.activeMatrixEditorsOutsideRole(adminRole)).isZero();
  }

  @Test
  void roleNameTakenIsCaseInsensitiveAcrossCodeAndName() {
    assertThat(repo.roleNameTaken("admin", "zzz")).isTrue();
    assertThat(repo.roleNameTaken("zzz", "quản trị")).isTrue();
    assertThat(repo.roleNameTaken("zzz", "instructor")).isTrue();
    assertThat(repo.roleNameTaken("zzz", "yyy")).isFalse();
  }

  @Test
  void insertRoleReturnsFalseOnDuplicateCodeAndWritesNothing() {
    var first = new RbacRole(UUID.randomUUID(), "TA", "Teaching assistant", "INSTRUCTOR", false);
    assertThat(repo.insertRole(first)).isTrue();
    assertThat(count("roles")).isEqualTo(4);
    assertThat(repo.insertRole(new RbacRole(UUID.randomUUID(), "TA", "Other", "STUDENT", false)))
        .isFalse();
    assertThat(count("roles")).isEqualTo(4);
    assertThat(
            jdbc.sql("SELECT is_system FROM identity.roles WHERE code='TA'")
                .query(Boolean.class)
                .single())
        .isFalse();
  }

  @Test
  void deleteRoleIsBlockedByUsersAndCascadesItsPermissions() {
    var custom = new RbacRole(UUID.randomUUID(), "TA", "Teaching assistant", "INSTRUCTOR", false);
    repo.insertRole(custom);
    repo.setGranted(custom.id(), functionId("CLASS_MANAGEMENT"), actionId("READ"), true, null);
    userInRole(custom.id(), "t@x.io", "ACTIVE");
    assertThat(repo.countUsers(custom.id())).isEqualTo(1);
    assertThat(repo.deleteRole(custom.id())).isFalse();
    assertThat(count("roles")).isEqualTo(4);
    assertThat(count("permissions")).isEqualTo(133);

    jdbc.sql("DELETE FROM identity.users WHERE role_id = :r").param("r", custom.id()).update();
    assertThat(repo.deleteRole(custom.id())).isTrue();
    assertThat(count("roles")).isEqualTo(3);
    assertThat(count("permissions")).isEqualTo(132);
    assertThat(repo.deleteRole(custom.id())).isFalse();
  }

  @Test
  void lockForPermissionChangeLocksAdminRowsAndTheRequestedRole() {
    UUID student = roleId("STUDENT");
    assertThat(repo.lockRole(UUID.randomUUID())).isEmpty();
    assertThat(repo.lockForPermissionChange(UUID.randomUUID())).isEmpty();

    tx.executeWithoutResult(
        status -> {
          assertThat(repo.lockForPermissionChange(student)).map(RbacRole::code).contains("STUDENT");
          assertThat(lockFailure("ADMIN")).isEqualTo("55P03");
          assertThat(lockFailure("STUDENT")).isEqualTo("55P03");
          assertThat(lockFailure("INSTRUCTOR")).isNull();
        });
    // After commit every lock is released.
    assertThat(lockFailure("ADMIN")).isNull();

    tx.executeWithoutResult(
        status -> {
          assertThat(repo.lockRole(student)).map(RbacRole::code).contains("STUDENT");
          assertThat(lockFailure("STUDENT")).isEqualTo("55P03");
          assertThat(lockFailure("ADMIN")).isNull();
        });
  }

  /** Tries a NOWAIT row lock on a second connection; returns the SQLSTATE or null if it got it. */
  private String lockFailure(String roleCode) {
    try (Connection other = data.getConnection()) {
      other.setAutoCommit(false);
      try (var statement =
          other.prepareStatement("SELECT 1 FROM identity.roles WHERE code = ? FOR UPDATE NOWAIT")) {
        statement.setString(1, roleCode);
        statement.execute();
        return null;
      } finally {
        other.rollback();
      }
    } catch (SQLException e) {
      return e.getSQLState();
    }
  }

  @Test
  void functionAndActionCodeLookups() {
    assertThat(repo.functionCode(functionId("AI_CONFIG"))).contains("AI_CONFIG");
    assertThat(repo.actionCode(actionId("DELETE"))).contains("DELETE");
    assertThat(repo.functionCode(UUID.randomUUID())).isEmpty();
    assertThat(repo.actionCode(UUID.randomUUID())).isEmpty();
  }
}
