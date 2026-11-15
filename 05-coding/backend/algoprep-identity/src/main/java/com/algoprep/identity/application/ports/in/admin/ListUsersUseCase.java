package com.algoprep.identity.application.ports.in.admin;

import com.algoprep.identity.application.dto.admin.ListUsersQuery;
import com.algoprep.identity.domain.model.admin.AdminUserPage;

/** Admin user list with filters and paging (#35). */
public interface ListUsersUseCase {
  AdminUserPage list(ListUsersQuery query);
}
