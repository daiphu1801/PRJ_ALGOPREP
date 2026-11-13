package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.identity.domain.model.CatalogEntry;
import com.algoprep.identity.domain.model.PermissionCell;
import com.algoprep.identity.domain.model.PermissionMatrix;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.model.RoleListItem;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** #30 and #32 hand the repository's answers through unchanged. */
class RbacQueryHandlersTest {
  private final RbacRepository rbac = mock(RbacRepository.class);

  @Test
  void theMatrixIsRolesFunctionsActionsAndCellsAsStored() {
    RbacRole role = new RbacRole(UUID.randomUUID(), "ADMIN", "Quản trị", "ADMIN", true);
    CatalogEntry function = new CatalogEntry(UUID.randomUUID(), "CLASS_MANAGEMENT", "Lớp");
    CatalogEntry action = new CatalogEntry(UUID.randomUUID(), "READ", null);
    PermissionCell cell = new PermissionCell(role.id(), function.id(), action.id(), true);
    when(rbac.roles()).thenReturn(List.of(role));
    when(rbac.functions()).thenReturn(List.of(function));
    when(rbac.actions()).thenReturn(List.of(action));
    when(rbac.cells()).thenReturn(List.of(cell));

    PermissionMatrix matrix = new GetPermissionMatrixQueryHandler(rbac).get();

    assertThat(matrix)
        .isEqualTo(
            new PermissionMatrix(List.of(role), List.of(function), List.of(action), List.of(cell)));
  }

  @Test
  void theRoleListCarriesTheUserCounts() {
    RoleListItem item =
        new RoleListItem(UUID.randomUUID(), "STUDENT", "Học viên", "STUDENT", true, 3);
    when(rbac.listRoles()).thenReturn(List.of(item));

    assertThat(new ListRolesQueryHandler(rbac).list()).containsExactly(item);
  }
}
