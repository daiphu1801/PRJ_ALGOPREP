package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.CatalogEntry;
import com.algoprep.identity.domain.model.PermissionCell;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.model.RoleListItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Roles, the seeded catalogue and the matrix cells of {@code identity} (#30 to #34). */
public interface RbacRepository {

  /** All roles with their account count, built-in roles first. */
  List<RoleListItem> listRoles();

  /** All roles as plain rows. */
  List<RbacRole> roles();

  /** The seeded functions. */
  List<CatalogEntry> functions();

  /** The seeded actions in CREATE, READ, UPDATE, DELETE order. */
  List<CatalogEntry> actions();

  /** Every role, function, action triple; triples without a stored row are not granted. */
  List<PermissionCell> cells();

  /**
   * Locks the target role and every ADMIN role, in id order, until the caller's transaction ends,
   * so two concurrent edits can neither lose a version bump nor both remove the last admin cell.
   *
   * @return the target, empty when it does not exist
   */
  Optional<RbacRole> lockForPermissionChange(UUID roleId);

  /** Code of a function, empty when unknown. */
  Optional<String> functionCode(UUID functionId);

  /** Code of an action, empty when unknown. */
  Optional<String> actionCode(UUID actionId);

  /** Stored value of a cell; a missing row reads as false. */
  boolean isGranted(UUID roleId, UUID functionId, UUID actionId);

  /** Writes the cell, creating its row when needed, stamped with the database clock. */
  void setGranted(UUID roleId, UUID functionId, UUID actionId, boolean granted, UUID actorId);

  /** Raises the role's permission version by one, which makes every later request re-read it. */
  void bumpPermissionVersion(UUID roleId);

  /**
   * Number of ACTIVE accounts, outside the given role, whose ADMIN role holds {@code
   * PERMISSION_MATRIX:UPDATE}: the people who could still repair the matrix (IDT-420).
   */
  int activeMatrixEditorsOutsideRole(UUID roleId);

  /** True when the role is an ADMIN role holding {@code PERMISSION_MATRIX:UPDATE}. */
  boolean isMatrixEditorRole(UUID roleId);

  /** True when a role has the same code or the same label, ignoring case. */
  boolean roleNameTaken(String code, String name);

  /**
   * Inserts a custom role (not built-in).
   *
   * @return false when the unique code was taken meanwhile
   */
  boolean insertRole(RbacRole role);

  /** Locks and returns a role, empty when it does not exist. */
  Optional<RbacRole> lockRole(UUID roleId);

  /** Accounts that currently hold the role. */
  int countUsers(UUID roleId);

  /**
   * Deletes the role and, by cascade, its cells.
   *
   * @return false when an account was assigned meanwhile
   */
  boolean deleteRole(UUID roleId);
}
