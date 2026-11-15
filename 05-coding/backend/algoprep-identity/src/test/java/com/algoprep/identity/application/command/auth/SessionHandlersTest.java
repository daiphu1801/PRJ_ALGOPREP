package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.IssuedAccessToken;
import com.algoprep.identity.application.dto.RefreshResult;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.RefreshTokenSecrets;
import com.algoprep.identity.domain.model.StoredRefreshToken;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit tests for session issuing, refresh rotation with reuse detection, and logout (BR-02). */
class SessionHandlersTest {
  private static final Instant NOW = Instant.parse("2026-10-31T00:00:00Z");
  private static final String RAW = "raw-refresh-token";

  private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
  private final UserAccountRepository users = mock(UserAccountRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final AccessTokenPort accessTokens = mock(AccessTokenPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final SessionIssuer issuer = new SessionIssuer(accessTokens, tokens, ids, clock, 30);
  private final RefreshSessionCommandHandler refresh =
      new RefreshSessionCommandHandler(tokens, users, audit, issuer, clock);
  private final LogoutCommandHandler logout = new LogoutCommandHandler(tokens, clock);

  private final UUID userId = UUID.randomUUID();
  private final UUID familyId = UUID.randomUUID();
  private final UserAccount active =
      new UserAccount(
          userId, "hash", "Name", UserStatus.ACTIVE, false, "STUDENT", BaseCategory.STUDENT);

  SessionHandlersTest() {
    when(clock.now()).thenReturn(NOW);
    when(ids.newId()).thenReturn(UUID.randomUUID());
    when(accessTokens.issue(
            org.mockito.ArgumentMatchers.eq(userId),
            org.mockito.ArgumentMatchers.eq(BaseCategory.STUDENT),
            org.mockito.ArgumentMatchers.eq("STUDENT"),
            org.mockito.ArgumentMatchers.any(UUID.class)))
        .thenReturn(new IssuedAccessToken("jwt", NOW.plus(Duration.ofMinutes(15))));
  }

  private StoredRefreshToken stored(Instant expiresAt, Instant revokedAt) {
    return new StoredRefreshToken(UUID.randomUUID(), userId, familyId, expiresAt, revokedAt);
  }

  private void storeFor(StoredRefreshToken token) {
    when(tokens.findByHashForUpdate(RefreshTokenSecrets.hash(RAW))).thenReturn(Optional.of(token));
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            UnauthenticatedException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void issuerStoresOnlyTheHashAndReportsRemainingLifetime() {
    SessionIssuer.Session session = issuer.openNewFamily(active);

    ArgumentCaptor<StoredRefreshToken> row = ArgumentCaptor.forClass(StoredRefreshToken.class);
    ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
    verify(tokens).insert(row.capture(), hash.capture(), org.mockito.ArgumentMatchers.eq(NOW));
    assertThat(hash.getValue()).isEqualTo(RefreshTokenSecrets.hash(session.rawRefreshToken()));
    assertThat(hash.getValue()).isNotEqualTo(session.rawRefreshToken());
    assertThat(row.getValue().expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    assertThat(row.getValue().revokedAt()).isNull();
    assertThat(session.expiresInSeconds()).isEqualTo(900);
    assertThat(session.accessToken().value()).isEqualTo("jwt");
  }

  @Test
  void blankOrUnknownRefreshTokenIsInvalidWithoutWrites() {
    assertThatThrownBy(() -> refresh.refresh(null))
        .satisfies(e -> assertCode(e, IdentityErrorCodes.REFRESH_TOKEN_INVALID));
    assertThatThrownBy(() -> refresh.refresh("  "))
        .satisfies(e -> assertCode(e, IdentityErrorCodes.REFRESH_TOKEN_INVALID));
    when(tokens.findByHashForUpdate(anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> refresh.refresh(RAW))
        .satisfies(e -> assertCode(e, IdentityErrorCodes.REFRESH_TOKEN_INVALID));
    verify(tokens, never()).revoke(any(), any());
    verifyNoInteractions(audit);
  }

  @Test
  void reusedTokenRevokesTheWholeFamilyAndWritesAnAuditRow() {
    StoredRefreshToken reused = stored(NOW.plus(Duration.ofDays(1)), NOW.minusSeconds(5));
    storeFor(reused);

    assertThatThrownBy(() -> refresh.refresh(RAW))
        .satisfies(e -> assertCode(e, IdentityErrorCodes.REFRESH_TOKEN_REUSED));

    verify(tokens).revokeFamily(familyId, NOW);
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("SECURITY_TOKEN_REUSE_DETECTED");
    assertThat(entry.getValue().actorUserId()).isEqualTo(userId);
    assertThat(entry.getValue().afterJson()).contains(familyId.toString());
  }

  @Test
  void expiredTokenIsInvalidAndNotRevokedAgain() {
    storeFor(stored(NOW, null));
    assertThatThrownBy(() -> refresh.refresh(RAW))
        .satisfies(e -> assertCode(e, IdentityErrorCodes.REFRESH_TOKEN_INVALID));
    verify(tokens, never()).revoke(any(), any());
  }

  @Test
  void ownerThatIsNoLongerActiveCannotRefresh() {
    storeFor(stored(NOW.plus(Duration.ofDays(1)), null));
    when(users.findById(userId))
        .thenReturn(
            Optional.of(
                new UserAccount(
                    userId,
                    "hash",
                    "Name",
                    UserStatus.DEACTIVATED,
                    false,
                    "STUDENT",
                    BaseCategory.STUDENT)));
    assertThatThrownBy(() -> refresh.refresh(RAW))
        .satisfies(e -> assertCode(e, IdentityErrorCodes.REFRESH_TOKEN_INVALID));
    verify(tokens, never()).revoke(any(), any());

    when(users.findById(userId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> refresh.refresh(RAW))
        .satisfies(e -> assertCode(e, IdentityErrorCodes.REFRESH_TOKEN_INVALID));
  }

  @Test
  void validRefreshRotatesWithinTheSameFamily() {
    StoredRefreshToken current = stored(NOW.plus(Duration.ofDays(1)), null);
    storeFor(current);
    when(users.findById(userId)).thenReturn(Optional.of(active));

    RefreshResult result = refresh.refresh(RAW);

    verify(tokens).revoke(current.id(), NOW);
    ArgumentCaptor<StoredRefreshToken> next = ArgumentCaptor.forClass(StoredRefreshToken.class);
    verify(tokens).insert(next.capture(), anyString(), org.mockito.ArgumentMatchers.eq(NOW));
    assertThat(next.getValue().familyId()).isEqualTo(familyId);
    assertThat(result.accessToken()).isEqualTo("jwt");
    assertThat(result.rawRefreshToken()).isNotBlank().isNotEqualTo(RAW);
  }

  @Test
  void logoutRevokesALiveTokenAndIgnoresEverythingElse() {
    logout.logout(null);
    logout.logout("");
    when(tokens.findByHashForUpdate(anyString())).thenReturn(Optional.empty());
    logout.logout(RAW);
    storeFor(stored(NOW.plus(Duration.ofDays(1)), NOW.minusSeconds(1)));
    logout.logout(RAW);
    verify(tokens, never()).revoke(any(), any());

    StoredRefreshToken live = stored(NOW.plus(Duration.ofDays(1)), null);
    storeFor(live);
    logout.logout(RAW);
    verify(tokens).revoke(live.id(), NOW);
  }
}
