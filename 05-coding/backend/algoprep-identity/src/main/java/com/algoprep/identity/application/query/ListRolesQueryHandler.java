package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ListRolesUseCase;
import com.algoprep.identity.domain.model.RoleListItem;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListRolesQueryHandler implements ListRolesUseCase {
  private final RbacRepository rbac;

  ListRolesQueryHandler(RbacRepository rbac) {
    this.rbac = rbac;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoleListItem> list() {
    return rbac.listRoles();
  }
}
