package com.algoprep.identity.infrastructure.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Turns a verified JWT into an authentication, re-reading the account on every request.
 *
 * <p>Two things must be live, not read from the token: the account status (a deleted or locked user
 * must lose access before the 15 minute token expires, BD security section 4) and the permission
 * matrix (a changed cell applies to the next request, DEC-2026-1028-qa-f1-permission-immediate).
 *
 * <p>Authorities produced: {@code ROLE_<BASE_CATEGORY>} for layer 1, and one {@code
 * FUNCTION:ACTION} per granted matrix cell for layer 2. Other modules gate with {@code
 * hasAuthority('FUNCTION:ACTION')} and import nothing from identity. The matrix is cached per role
 * and reused only while {@code roles.permission_version} read from PostgreSQL still matches; if the
 * database cannot be read the request fails, never falls back to a stale cache.
 */
@Component
class IdentityJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final String ACCOUNT_SQL =
      """
      SELECT u.status, r.id AS role_id, r.base_category, r.permission_version
      FROM identity.users u JOIN identity.roles r ON r.id = u.role_id
      WHERE u.id = :id
      """;

  private static final String MATRIX_SQL =
      """
      SELECT f.code || ':' || a.code
      FROM identity.permissions p
      JOIN identity.functions f ON f.id = p.function_id
      JOIN identity.actions a ON a.id = p.action_id
      WHERE p.role_id = :roleId AND p.granted
      """;

  private final ObjectProvider<JdbcClient> jdbcProvider;
  private final ConcurrentMap<UUID, CachedMatrix> matrixByRole = new ConcurrentHashMap<>();

  IdentityJwtAuthenticationConverter(ObjectProvider<JdbcClient> jdbcProvider) {
    this.jdbcProvider = jdbcProvider;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    JdbcClient jdbc = jdbcProvider.getObject();
    UUID userId = parseSubject(jwt);
    Account account =
        jdbc.sql(ACCOUNT_SQL)
            .param("id", userId)
            .query(
                (rs, n) ->
                    new Account(
                        rs.getString("status"),
                        rs.getObject("role_id", UUID.class),
                        rs.getString("base_category"),
                        rs.getLong("permission_version")))
            .optional()
            .orElseThrow(() -> new InvalidBearerTokenException("Unknown account"));
    if (!"ACTIVE".equals(account.status())) {
      throw new InvalidBearerTokenException("Account is not active");
    }
    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + account.baseCategory()));
    for (String cell : matrix(jdbc, account)) {
      authorities.add(new SimpleGrantedAuthority(cell));
    }
    return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
  }

  private Set<String> matrix(JdbcClient jdbc, Account account) {
    CachedMatrix cached = matrixByRole.get(account.roleId());
    if (cached != null && cached.version() == account.permissionVersion()) {
      return cached.cells();
    }
    Set<String> cells =
        Set.copyOf(
            jdbc.sql(MATRIX_SQL).param("roleId", account.roleId()).query(String.class).list());
    matrixByRole.put(account.roleId(), new CachedMatrix(account.permissionVersion(), cells));
    return cells;
  }

  private static UUID parseSubject(Jwt jwt) {
    String subject = jwt.getSubject();
    if (subject == null) {
      throw new InvalidBearerTokenException("Missing subject");
    }
    try {
      return UUID.fromString(subject);
    } catch (IllegalArgumentException e) {
      throw new InvalidBearerTokenException("Malformed subject", e);
    }
  }

  private record Account(String status, UUID roleId, String baseCategory, long permissionVersion) {}

  private record CachedMatrix(long version, Set<String> cells) {}
}
