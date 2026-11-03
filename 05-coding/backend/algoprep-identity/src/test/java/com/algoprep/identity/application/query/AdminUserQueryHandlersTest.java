package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.ListUsersQuery;
import com.algoprep.identity.domain.model.UserGroupShare;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** #35, #62, #63: parameter rules and the one-decimal shares. */
class AdminUserQueryHandlersTest {
  private final AdminUserRepository users = mock(AdminUserRepository.class);
  private final ListUsersQueryHandler list = new ListUsersQueryHandler(users);

  @Test
  void blankAndAllFiltersMeanAnyAndDefaultsApply() {
    list.list(new ListUsersQuery("ALL", " ", "  ", null, null));
    verify(users).search(null, null, null, 1, 20);

    list.list(new ListUsersQuery("ACTIVE", "ADMIN", " kw ", 3, 100));
    verify(users).search("ACTIVE", "ADMIN", "kw", 3, 100);
  }

  @Test
  void everyBadParameterIsIdtV17WithoutAQuery() {
    for (ListUsersQuery bad :
        List.of(
            new ListUsersQuery("NOPE", null, null, null, null),
            new ListUsersQuery(null, "NOPE", null, null, null),
            new ListUsersQuery(null, null, "x".repeat(101), null, null),
            new ListUsersQuery(null, null, null, 0, null),
            new ListUsersQuery(null, null, null, null, 0),
            new ListUsersQuery(null, null, null, null, 101))) {
      assertThatThrownBy(() -> list.list(bad))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-V17"));
    }
    verify(users, never()).search(any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void theStatsHandlerReturnsThePendingCount() {
    when(users.countPendingVerification()).thenReturn(7L);

    assertThat(new GetUserManagementStatsQueryHandler(users).pendingVerificationCount())
        .isEqualTo(7L);
  }

  @Test
  void sharesAreRoundedToOneDecimalAndAZeroTotalGivesZeros() {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("STUDENT", 1L);
    counts.put("INSTRUCTOR", 1L);
    counts.put("ADMIN", 1L);
    counts.put("DEACTIVATED", 0L);
    when(users.groupCounts()).thenReturn(counts);
    GetRoleDistributionQueryHandler handler = new GetRoleDistributionQueryHandler(users);

    List<UserGroupShare> thirds = handler.distribution();

    assertThat(thirds)
        .extracting(UserGroupShare::groupCode)
        .containsExactly("STUDENT", "INSTRUCTOR", "ADMIN", "DEACTIVATED");
    assertThat(thirds).extracting(UserGroupShare::percent).containsExactly(33.3, 33.3, 33.3, 0.0);

    counts.replaceAll((k, v) -> 0L);
    assertThat(handler.distribution()).extracting(UserGroupShare::percent).containsOnly(0.0);
  }
}
