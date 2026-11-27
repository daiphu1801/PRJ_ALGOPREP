package com.algoprep.identity.application.ports.in.rbac;

import com.algoprep.identity.domain.model.rbac.RoleListItem;
import java.util.List;

/** Lists built-in and custom roles with their account counts (#32). */
public interface ListRolesUseCase {
  List<RoleListItem> list();
}
