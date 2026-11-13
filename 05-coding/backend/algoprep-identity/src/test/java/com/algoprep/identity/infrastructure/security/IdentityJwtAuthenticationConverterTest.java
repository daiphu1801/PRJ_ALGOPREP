package com.algoprep.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/**
 * The database-failure and malformed-subject paths; the happy paths run in SecurityFoundationTest.
 */
class IdentityJwtAuthenticationConverterTest {
  @SuppressWarnings("unchecked")
  private final ObjectProvider<JdbcClient> provider = mock(ObjectProvider.class);

  private final JdbcClient jdbc = mock(JdbcClient.class);
  private final IdentityJwtAuthenticationConverter converter =
      new IdentityJwtAuthenticationConverter(provider);

  private static Jwt jwt(String subject) {
    Jwt.Builder builder =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60));
    if (subject != null) {
      builder.subject(subject);
    } else {
      builder.claim("scope", "none");
    }
    return builder.build();
  }

  @Test
  void databaseFailureIsRejectedAsUnavailableWithTheDataAccessCauseKept() {
    when(provider.getObject()).thenReturn(jdbc);
    when(jdbc.sql(anyString())).thenThrow(new DataAccessResourceFailureException("db down"));

    assertThatThrownBy(() -> converter.convert(jwt(UUID.randomUUID().toString())))
        .isInstanceOf(InvalidBearerTokenException.class)
        .hasCauseInstanceOf(DataAccessException.class);
  }

  @Test
  void missingOrMalformedSubjectIsRejectedBeforeTouchingTheDatabase() {
    assertThatThrownBy(() -> converter.convert(jwt(null)))
        .isInstanceOf(InvalidBearerTokenException.class)
        .satisfies(e -> assertThat(e.getCause()).isNull());
    assertThatThrownBy(() -> converter.convert(jwt("not-a-uuid")))
        .isInstanceOf(InvalidBearerTokenException.class);
    verifyNoInteractions(provider, jdbc);
  }
}
