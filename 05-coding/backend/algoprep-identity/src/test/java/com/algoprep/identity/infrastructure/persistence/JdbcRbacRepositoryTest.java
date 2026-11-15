package com.algoprep.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.domain.model.rbac.RbacRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F1 RBAC matrix persistence contract: role list, catalogue, cells, lock and custom-role CRUD. */
class JdbcRbacRepositoryTest {
  private final FakeDb db = new FakeDb();
  private final UUID generated = UUID.randomUUID();
  private final JdbcRbacRepository repo = new JdbcRbacRepository(db.provider(), () -> generated);
  private final UUID roleId = UUID.randomUUID();

  @Test
  void listRolesMapsRowsWithUserCount() {
    db.rows(
        "FROM identity.roles r ORDER BY",
        Map.of(
            "id",
            roleId,
            "code",
            "ADMIN",
            "name",
            "Admin",
            "base_category",
            "ADMIN",
            "is_system",
            true,
            "user_count",
            3));
    var items = repo.listRoles();
    assertThat(items).hasSize(1);
    assertThat(items.get(0).code()).isEqualTo("ADMIN");
    assertThat(items.get(0).userCount()).isEqualTo(3);
    assertThat(items.get(0).isSystem()).isTrue();
  }

  @Test
  void rolesFunctionsActionsAndCellsReturnMappedRows() {
    db.generic("FROM identity.roles ORDER BY", 2)
        .generic("FROM identity.functions ORDER BY", 1)
        .generic("FROM identity.actions ORDER BY", 4)
        .generic("CROSS JOIN identity.functions", 3);
    assertThat(repo.roles()).hasSize(2);
    assertThat(repo.functions()).hasSize(1);
    assertThat(repo.actions()).hasSize(4);
    assertThat(repo.actions().get(0).name()).isNull();
    assertThat(repo.cells()).hasSize(3);
  }

  @Test
  void listsAreEmptyWhenNothingSeeded() {
    assertThat(repo.roles()).isEmpty();
    assertThat(repo.cells()).isEmpty();
  }

  @Test
  void lockForPermissionChangeKeepsOnlyTheRequestedRole() {
    UUID other = UUID.randomUUID();
    db.rows(
        "FOR UPDATE",
        Map.of("id", other, "code", "ADMIN", "name", "A", "base_category", "ADMIN"),
        Map.of("id", roleId, "code", "X", "name", "X", "base_category", "STUDENT"));
    assertThat(repo.lockForPermissionChange(roleId)).map(RbacRole::code).contains("X");
    assertThat(db.last().values()).contains(roleId);
  }

  @Test
  void lockForPermissionChangeIsEmptyWhenRoleMissing() {
    db.rows("FOR UPDATE", Map.of("id", UUID.randomUUID(), "code", "ADMIN"));
    assertThat(repo.lockForPermissionChange(roleId)).isEmpty();
  }

  @Test
  void codeLookupsReturnOptional() {
    db.rows("SELECT code FROM identity.functions", Map.of("code", "PERMISSION_MATRIX"));
    assertThat(repo.functionCode(roleId)).contains("PERMISSION_MATRIX");
    assertThat(repo.actionCode(roleId)).isEmpty();
  }

  @Test
  void isGrantedDefaultsToFalseWhenNoCell() {
    assertThat(repo.isGranted(roleId, roleId, roleId)).isFalse();
    db.rows("SELECT granted", Map.of("granted", true));
    assertThat(repo.isGranted(roleId, roleId, roleId)).isTrue();
  }

  @Test
  void setGrantedUpsertsWithGeneratedIdAndActor() {
    UUID actor = UUID.randomUUID();
    repo.setGranted(roleId, roleId, roleId, true, actor);
    assertThat(db.last().sql()).contains("ON CONFLICT (role_id, function_id, action_id)");
    assertThat(db.last().values()).contains(generated, actor, true);
  }

  @Test
  void bumpPermissionVersionIncrementsRole() {
    repo.bumpPermissionVersion(roleId);
    assertThat(db.last().sql()).contains("permission_version = permission_version + 1");
    assertThat(db.last().values()).containsExactly(roleId);
  }

  @Test
  void matrixEditorChecks() {
    db.rows("SELECT count(*) FROM identity.users u", Map.of("count", 2));
    db.rows("SELECT EXISTS (SELECT 1 FROM identity.roles r", Map.of("exists", true));
    assertThat(repo.activeMatrixEditorsOutsideRole(roleId)).isEqualTo(2);
    assertThat(repo.isMatrixEditorRole(roleId)).isTrue();
  }

  @Test
  void roleNameTakenReflectsExistence() {
    db.rows("lower(code) = lower(?)", Map.of("exists", true));
    assertThat(repo.roleNameTaken("C", "N")).isTrue();
    assertThat(db.last().values()).contains("C", "N");
  }

  @Test
  void insertRoleReturnsTrueAndFalseOnDuplicate() {
    RbacRole role = new RbacRole(roleId, "C", "N", "STUDENT", false);
    assertThat(repo.insertRole(role)).isTrue();
    db.fail("INSERT INTO identity.roles", "23505");
    assertThat(repo.insertRole(role)).isFalse();
  }

  @Test
  void lockRoleAndCountUsers() {
    db.rows("WHERE id = ? FOR UPDATE", Map.of("id", roleId, "code", "C"));
    db.rows("SELECT count(*) FROM identity.users WHERE", Map.of("count", 5));
    assertThat(repo.lockRole(roleId)).isPresent();
    assertThat(repo.countUsers(roleId)).isEqualTo(5);
  }

  @Test
  void deleteRoleDependsOnRowCountAndSwallowsForeignKeyViolation() {
    assertThat(repo.deleteRole(roleId)).isTrue();
    db.updateCount("DELETE FROM identity.roles", 0);
    assertThat(repo.deleteRole(roleId)).isFalse();
    db.fail("DELETE FROM identity.roles", "23503");
    assertThat(repo.deleteRole(roleId)).isFalse();
  }
}
