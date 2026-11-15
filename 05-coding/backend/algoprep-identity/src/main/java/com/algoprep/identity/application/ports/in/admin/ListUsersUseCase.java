package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.ListUsersQuery;
import com.algoprep.identity.domain.model.AdminUserPage;

/** Admin user list with filters and paging (#35). */
public interface ListUsersUseCase {
  AdminUserPage list(ListUsersQuery query);
}
