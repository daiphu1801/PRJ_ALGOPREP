package com.algoprep.identity.application.query;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.ListUsersQuery;
import com.algoprep.identity.application.ports.in.ListUsersUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AdminUserPage;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin user list (#35). Parameters are checked here whatever the client did; every bad one is
 * {@code 400 IDT-V17}. {@code role} is the layer-1 category of the account's role.
 */
@Service
class ListUsersQueryHandler implements ListUsersUseCase {
  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;
  static final int KEYWORD_MAX = 100;
  private static final Set<String> STATUSES =
      Set.of("ACTIVE", "DEACTIVATED", "PENDING_VERIFICATION");
  private static final Set<String> CATEGORIES = Set.of("STUDENT", "INSTRUCTOR", "ADMIN");

  private final AdminUserRepository users;

  ListUsersQueryHandler(AdminUserRepository users) {
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminUserPage list(ListUsersQuery query) {
    String status = oneOf(query.status(), STATUSES);
    String category = oneOf(query.role(), CATEGORIES);
    String keyword = query.keyword() == null ? "" : query.keyword().trim();
    int page = query.page() == null ? 1 : query.page();
    int pageSize = query.pageSize() == null ? DEFAULT_PAGE_SIZE : query.pageSize();
    if (keyword.length() > KEYWORD_MAX || page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw invalid();
    }
    return users.search(status, category, keyword.isEmpty() ? null : keyword, page, pageSize);
  }

  /** Blank and {@code ALL} mean "no filter"; anything else must be one of the values. */
  private static String oneOf(String raw, Set<String> allowed) {
    String value = raw == null ? "" : raw.trim();
    if (value.isEmpty() || "ALL".equals(value)) {
      return null;
    }
    if (!allowed.contains(value)) {
      throw invalid();
    }
    return value;
  }

  private static InvalidRequestException invalid() {
    return new InvalidRequestException(
        IdentityErrorCodes.LIST_PARAMETER_INVALID, "A filter or paging value is not valid");
  }
}
