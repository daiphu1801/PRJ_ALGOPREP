package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.domain.model.CatalogEntry;
import com.algoprep.identity.domain.model.PermissionCell;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.model.RoleListItem;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Roles, catalogue and cells of {@code identity}; timestamps come from the database clock. */
@Repository
class JdbcRbacRepository implements RbacRepository {
  private static final String ROLE_COLUMNS = "id, code, name, base_category, is_system";

  private final ObjectProvider<JdbcClient> jdbc;
  private final IdGeneratorPort ids;

  JdbcRbacRepository(ObjectProvider<JdbcClient> jdbc, IdGeneratorPort ids) {
    this.jdbc = jdbc;
    this.ids = ids;
  }

  @Override
  public List<RoleListItem> listRoles() {
    return jdbc.getObject()
        .sql(
            "SELECT r.id, r.code, r.name, r.base_category, r.is_system,"
                + " (SELECT count(*) FROM identity.users u WHERE u.role_id = r.id) AS user_count"
                + " FROM identity.roles r ORDER BY r.is_system DESC, r.created_at, r.code")
        .query(
            (rs, n) ->
                new RoleListItem(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getString("base_category"),
                    rs.getBoolean("is_system"),
                    rs.getInt("user_count")))
        .list();
  }

  @Override
  public List<RbacRole> roles() {
    return jdbc.getObject()
        .sql(
            "SELECT "
                + ROLE_COLUMNS
                + " FROM identity.roles ORDER BY is_system DESC, created_at, code")
        .query(JdbcRbacRepository::role)
        .list();
  }

  @Override
  public List<CatalogEntry> functions() {
    return jdbc.getObject()
        .sql("SELECT id, code, name FROM identity.functions ORDER BY code")
        .query(
            (rs, n) ->
                new CatalogEntry(
                    rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name")))
        .list();
  }

  @Override
  public List<CatalogEntry> actions() {
    return jdbc.getObject()
        .sql(
            "SELECT id, code FROM identity.actions ORDER BY CASE code WHEN 'CREATE' THEN 1"
                + " WHEN 'READ' THEN 2 WHEN 'UPDATE' THEN 3 ELSE 4 END")
        .query(
            (rs, n) -> new CatalogEntry(rs.getObject("id", UUID.class), rs.getString("code"), null))
        .list();
  }

  @Override
  public List<PermissionCell> cells() {
    return jdbc.getObject()
        .sql(
            "SELECT r.id AS role_id, f.id AS function_id, a.id AS action_id,"
                + " COALESCE(p.granted, false) AS granted"
                + " FROM identity.roles r CROSS JOIN identity.functions f"
                + " CROSS JOIN identity.actions a LEFT JOIN identity.permissions p"
                + " ON p.role_id = r.id AND p.function_id = f.id AND p.action_id = a.id"
                + " ORDER BY r.code, f.code, a.code")
        .query(
            (rs, n) ->
                new PermissionCell(
                    rs.getObject("role_id", UUID.class),
                    rs.getObject("function_id", UUID.class),
                    rs.getObject("action_id", UUID.class),
                    rs.getBoolean("granted")))
        .list();
  }

  @Override
  public Optional<RbacRole> lockForPermissionChange(UUID roleId) {
    return jdbc
        .getObject()
        .sql(
            "SELECT "
                + ROLE_COLUMNS
                + " FROM identity.roles WHERE id = :id OR base_category = 'ADMIN'"
                + " ORDER BY id FOR UPDATE")
        .param("id", roleId)
        .query(JdbcRbacRepository::role)
        .list()
        .stream()
        .filter(role -> role.id().equals(roleId))
        .findFirst();
  }

  @Override
  public Optional<String> functionCode(UUID functionId) {
    return jdbc.getObject()
        .sql("SELECT code FROM identity.functions WHERE id = :id")
        .param("id", functionId)
        .query(String.class)
        .optional();
  }

  @Override
  public Optional<String> actionCode(UUID actionId) {
    return jdbc.getObject()
        .sql("SELECT code FROM identity.actions WHERE id = :id")
        .param("id", actionId)
        .query(String.class)
        .optional();
  }

  @Override
  public boolean isGranted(UUID roleId, UUID functionId, UUID actionId) {
    return jdbc.getObject()
        .sql(
            "SELECT granted FROM identity.permissions"
                + " WHERE role_id = :role AND function_id = :function AND action_id = :action")
        .param("role", roleId)
        .param("function", functionId)
        .param("action", actionId)
        .query(Boolean.class)
        .optional()
        .orElse(false);
  }

  @Override
  public void setGranted(
      UUID roleId, UUID functionId, UUID actionId, boolean granted, UUID actorId) {
    jdbc.getObject()
        .sql(
            "INSERT INTO identity.permissions"
                + " (id, role_id, function_id, action_id, granted, updated_at, updated_by)"
                + " VALUES (:id, :role, :function, :action, :granted, clock_timestamp(), :actor)"
                + " ON CONFLICT (role_id, function_id, action_id) DO UPDATE SET"
                + " granted = EXCLUDED.granted, updated_at = clock_timestamp(),"
                + " updated_by = EXCLUDED.updated_by")
        .param("id", ids.newId())
        .param("role", roleId)
        .param("function", functionId)
        .param("action", actionId)
        .param("granted", granted)
        .param("actor", actorId)
        .update();
  }

  @Override
  public void bumpPermissionVersion(UUID roleId) {
    jdbc.getObject()
        .sql(
            "UPDATE identity.roles SET permission_version = permission_version + 1,"
                + " updated_at = clock_timestamp() WHERE id = :id")
        .param("id", roleId)
        .update();
  }

  @Override
  public int activeMatrixEditorsOutsideRole(UUID roleId) {
    return jdbc.getObject()
        .sql(
            "SELECT count(*) FROM identity.users u"
                + " JOIN identity.roles r ON r.id = u.role_id"
                + " JOIN identity.permissions p ON p.role_id = r.id AND p.granted"
                + " JOIN identity.functions f ON f.id = p.function_id"
                + " JOIN identity.actions a ON a.id = p.action_id"
                + " WHERE u.status = 'ACTIVE' AND r.base_category = 'ADMIN' AND r.id <> :id"
                + " AND f.code = 'PERMISSION_MATRIX' AND a.code = 'UPDATE'")
        .param("id", roleId)
        .query(Integer.class)
        .single();
  }

  @Override
  public boolean isMatrixEditorRole(UUID roleId) {
    return jdbc.getObject()
        .sql(
            "SELECT EXISTS (SELECT 1 FROM identity.roles r"
                + " JOIN identity.permissions p ON p.role_id = r.id AND p.granted"
                + " JOIN identity.functions f ON f.id = p.function_id"
                + " JOIN identity.actions a ON a.id = p.action_id"
                + " WHERE r.id = :id AND r.base_category = 'ADMIN'"
                + " AND f.code = 'PERMISSION_MATRIX' AND a.code = 'UPDATE')")
        .param("id", roleId)
        .query(Boolean.class)
        .single();
  }

  @Override
  public boolean roleNameTaken(String code, String name) {
    return jdbc.getObject()
        .sql(
            "SELECT EXISTS (SELECT 1 FROM identity.roles WHERE lower(code) = lower(:code)"
                + " OR lower(name) = lower(:name) OR lower(code) = lower(:name))")
        .param("code", code)
        .param("name", name)
        .query(Boolean.class)
        .single();
  }

  @Override
  public boolean insertRole(RbacRole role) {
    try {
      jdbc.getObject()
          .sql(
              "INSERT INTO identity.roles (id, code, name, base_category, is_system, created_at,"
                  + " updated_at) VALUES (:id, :code, :name, :category, false, clock_timestamp(),"
                  + " clock_timestamp())")
          .param("id", role.id())
          .param("code", role.code())
          .param("name", role.name())
          .param("category", role.baseCategory())
          .update();
      return true;
    } catch (DuplicateKeyException e) {
      return false;
    }
  }

  @Override
  public Optional<RbacRole> lockRole(UUID roleId) {
    return jdbc.getObject()
        .sql("SELECT " + ROLE_COLUMNS + " FROM identity.roles WHERE id = :id FOR UPDATE")
        .param("id", roleId)
        .query(JdbcRbacRepository::role)
        .optional();
  }

  @Override
  public int countUsers(UUID roleId) {
    return jdbc.getObject()
        .sql("SELECT count(*) FROM identity.users WHERE role_id = :id")
        .param("id", roleId)
        .query(Integer.class)
        .single();
  }

  @Override
  public boolean deleteRole(UUID roleId) {
    try {
      return jdbc.getObject()
              .sql("DELETE FROM identity.roles WHERE id = :id")
              .param("id", roleId)
              .update()
          == 1;
    } catch (DataIntegrityViolationException e) {
      return false;
    }
  }

  private static RbacRole role(ResultSet rs, int row) throws SQLException {
    return new RbacRole(
        rs.getObject("id", UUID.class),
        rs.getString("code"),
        rs.getString("name"),
        rs.getString("base_category"),
        rs.getBoolean("is_system"));
  }
}
