package com.algoprep.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.infrastructure.persistence.FakeDb;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/** Token to authorities: role from base category, cells from the matrix, cache by version. */
class IdentityJwtAuthenticationConverterDbTest {
  private final UUID user = UUID.randomUUID();
  private final UUID role = UUID.randomUUID();

  private Jwt jwt() {
    return Jwt.withTokenValue("t")
        .header("alg", "RS256")
        .subject(user.toString())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }

  private static java.util.Set<String> names(org.springframework.security.core.Authentication a) {
    return a.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }

  @Test
  void activeAccountGetsRoleAndMatrixAuthorities() {
    FakeDb db =
        new FakeDb()
            .rows(
                "FROM identity.users u JOIN identity.roles r",
                Map.of(
                    "status",
                    "ACTIVE",
                    "role_id",
                    role,
                    "base_category",
                    "ADMIN",
                    "permission_version",
                    3L))
            .rows("FROM identity.permissions p", Map.of("cell", "USER_MANAGEMENT:READ"));
    var converter = new IdentityJwtAuthenticationConverter(db.provider());
    var auth = converter.convert(jwt());
    assertThat(names(auth)).containsExactlyInAnyOrder("ROLE_ADMIN", "USER_MANAGEMENT:READ");
    assertThat(auth.getName()).isEqualTo(user.toString());
  }

  @Test
  void matrixIsCachedPerRoleUntilThePermissionVersionChanges() {
    FakeDb db =
        new FakeDb()
            .rows(
                "FROM identity.users u JOIN identity.roles r",
                Map.of(
                    "status",
                    "ACTIVE",
                    "role_id",
                    role,
                    "base_category",
                    "STUDENT",
                    "permission_version",
                    1L))
            .rows("FROM identity.permissions p", Map.of("cell", "PROBLEM:READ"));
    var converter = new IdentityJwtAuthenticationConverter(db.provider());
    converter.convert(jwt());
    converter.convert(jwt());
    assertThat(db.calls().stream().filter(c -> c.sql().contains("identity.permissions")).count())
        .isEqualTo(1);

    FakeDb bumped =
        new FakeDb()
            .rows(
                "FROM identity.users u JOIN identity.roles r",
                Map.of(
                    "status",
                    "ACTIVE",
                    "role_id",
                    role,
                    "base_category",
                    "STUDENT",
                    "permission_version",
                    2L));
    assertThat(names(new IdentityJwtAuthenticationConverter(bumped.provider()).convert(jwt())))
        .containsExactly("ROLE_STUDENT");
  }

  @Test
  void unknownAndInactiveAccountsAreRejected() {
    var unknown = new IdentityJwtAuthenticationConverter(new FakeDb().provider());
    assertThatThrownBy(() -> unknown.convert(jwt()))
        .isInstanceOf(InvalidBearerTokenException.class)
        .hasMessageContaining("Unknown account");
    FakeDb deactivated =
        new FakeDb()
            .rows(
                "FROM identity.users u JOIN identity.roles r",
                Map.of(
                    "status",
                    "DEACTIVATED",
                    "role_id",
                    role,
                    "base_category",
                    "STUDENT",
                    "permission_version",
                    1L));
    var inactive = new IdentityJwtAuthenticationConverter(deactivated.provider());
    assertThatThrownBy(() -> inactive.convert(jwt()))
        .isInstanceOf(InvalidBearerTokenException.class)
        .hasMessageContaining("not active");
  }
}
