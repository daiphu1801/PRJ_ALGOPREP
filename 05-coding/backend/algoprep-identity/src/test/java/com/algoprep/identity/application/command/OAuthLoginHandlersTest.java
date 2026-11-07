package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.ports.out.OAuthProviderPort;
import com.algoprep.identity.application.ports.out.OAuthStatePort;
import com.algoprep.identity.domain.exception.OAuthCodeRejectedException;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.OAuthProfile;
import com.algoprep.identity.domain.model.OAuthProvider;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.OAuthAccountRepository;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OAuthLoginHandlersTest {
  private final OAuthProviderPort providers = mock(OAuthProviderPort.class);
  private final OAuthStatePort states = mock(OAuthStatePort.class);
  private final OAuthAccountRepository accounts = mock(OAuthAccountRepository.class);
  private final UserAccountRepository users = mock(UserAccountRepository.class);
  private final LoginAccountService admission = mock(LoginAccountService.class);
  private final SessionIssuer sessions = mock(SessionIssuer.class);
  private final CompleteOAuthLoginCommandHandler complete =
      new CompleteOAuthLoginCommandHandler(
          new OAuthExchangeService(providers, states), accounts, users, admission, sessions);
  private final StartOAuthLoginCommandHandler start =
      new StartOAuthLoginCommandHandler(providers, states);

  private final UUID id = UUID.randomUUID();

  private static OAuthProfile profile(boolean verified) {
    return new OAuthProfile(OAuthProvider.GITHUB, "gh-1", "an@example.test", verified, "Nguyen An");
  }

  private static UserAccount user(UUID id, UserStatus status, boolean mustChange) {
    return new UserAccount(
        id, null, "Nguyen An", status, mustChange, "STUDENT", BaseCategory.STUDENT);
  }

  private static String code(AlgoPrepException e) {
    return e.errorCode();
  }

  // ---- #5

  @Test
  void startStoresAFreshStateAndReturnsTheProviderUrl() {
    when(providers.authorizationUrl(eq(OAuthProvider.GOOGLE), anyString()))
        .thenAnswer(call -> Optional.of("https://idp/auth?state=" + call.getArgument(1)));

    var result = start.start("Google");

    assertThat(result.state()).hasSizeGreaterThan(30);
    assertThat(result.authorizationUrl()).endsWith(result.state());
    verify(states).store(result.state());
  }

  @Test
  void startRejectsAnUnknownProviderAndAnUnconfiguredOne() {
    assertThatThrownBy(() -> start.start("facebook"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(code(e)).isEqualTo("IDT-V28"));
    when(providers.authorizationUrl(any(), anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> start.start("github"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(code(e)).isEqualTo("IDT-505"));
    verify(states, never()).store(anyString());
  }

  // ---- #6 exchange

  @Test
  void aMissingOrUnknownStateIs401BeforeTheProviderIsCalled() {
    when(states.consume("known")).thenReturn(false);

    for (String state : new String[] {null, "", "known"}) {
      assertThatThrownBy(() -> complete.complete("github", "c", state))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, e -> assertThat(code(e)).isEqualTo("IDT-109"));
    }
    assertThatThrownBy(() -> complete.complete("github", " ", "s"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(code(e)).isEqualTo("IDT-109"));
    verifyNoInteractions(providers, accounts);
  }

  @Test
  void aRejectedCodeIs401AndAProviderFailureIs503() {
    when(states.consume("s")).thenReturn(true);
    when(providers.fetchProfile(any(), anyString())).thenThrow(new OAuthCodeRejectedException());
    assertThatThrownBy(() -> complete.complete("github", "c", "s"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(code(e)).isEqualTo("IDT-109"));

    org.mockito.Mockito.doThrow(new IllegalStateException("down"))
        .when(providers)
        .fetchProfile(any(), anyString());
    assertThatThrownBy(() -> complete.complete("github", "c", "s"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(code(e)).isEqualTo("IDT-505"));
    verifyNoInteractions(accounts);
  }

  @Test
  void anUnverifiedProviderEmailIs422AndTouchesNoAccount() {
    when(states.consume("s")).thenReturn(true);
    when(providers.fetchProfile(any(), anyString())).thenReturn(profile(false));

    assertThatThrownBy(() -> complete.complete("github", "c", "s"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(code(e)).isEqualTo("IDT-110"));
    verifyNoInteractions(accounts);
  }

  // ---- #6 accounts

  private void verified() {
    when(states.consume("s")).thenReturn(true);
    when(providers.fetchProfile(any(), anyString())).thenReturn(profile(true));
  }

  private void session(UUID userId) {
    var issued = mock(SessionIssuer.Session.class);
    when(issued.rawRefreshToken()).thenReturn("raw");
    when(issued.accessToken())
        .thenReturn(mock(com.algoprep.identity.application.dto.IssuedAccessToken.class));
    when(sessions.openNewFamily(any())).thenReturn(issued);
  }

  @Test
  void aLinkedProviderAccountSignsInWithoutCreatingOrLinkingAnything() {
    verified();
    when(accounts.findLinkedUser(OAuthProvider.GITHUB, "gh-1")).thenReturn(Optional.of(id));
    when(users.findById(id)).thenReturn(Optional.of(user(id, UserStatus.ACTIVE, false)));
    session(id);

    var result = complete.complete("github", "c", "s");

    assertThat(result.rawRefreshToken()).isEqualTo("raw");
    verify(accounts, never()).createStudent(anyString(), anyString());
    verify(accounts, never()).link(any(), any(), anyString(), anyString());
    verify(admission).admitByStatus(any());
    verify(admission).touchLastActive(id);
  }

  @Test
  void aVerifiedEmailOfAnExistingAccountIsLinkedNotDuplicated() {
    verified();
    when(accounts.findLinkedUser(any(), anyString())).thenReturn(Optional.empty());
    when(accounts.lockUserByEmail("an@example.test")).thenReturn(Optional.of(id));
    when(users.findById(id)).thenReturn(Optional.of(user(id, UserStatus.ACTIVE, false)));
    when(accounts.link(id, OAuthProvider.GITHUB, "gh-1", "an@example.test")).thenReturn(true);
    session(id);

    complete.complete("github", "c", "s");

    verify(accounts).link(id, OAuthProvider.GITHUB, "gh-1", "an@example.test");
    verify(accounts, never()).createStudent(anyString(), anyString());
    verify(accounts, never()).claimPendingAccount(any());
  }

  @Test
  void anUnknownEmailCreatesAStudentAndLinksIt() {
    verified();
    when(accounts.findLinkedUser(any(), anyString())).thenReturn(Optional.empty());
    when(accounts.lockUserByEmail(anyString())).thenReturn(Optional.empty());
    when(accounts.createStudent("an@example.test", "Nguyen An")).thenReturn(Optional.of(id));
    when(accounts.link(eq(id), any(), anyString(), anyString())).thenReturn(true);
    when(users.findById(id)).thenReturn(Optional.of(user(id, UserStatus.ACTIVE, false)));
    session(id);

    complete.complete("github", "c", "s");

    verify(accounts).createStudent("an@example.test", "Nguyen An");
    verify(accounts).link(id, OAuthProvider.GITHUB, "gh-1", "an@example.test");
  }

  @Test
  void aPendingSelfRegisteredAccountIsClaimedSoItsUnverifiedPasswordDies() {
    verified();
    when(accounts.findLinkedUser(any(), anyString())).thenReturn(Optional.empty());
    when(accounts.lockUserByEmail(anyString())).thenReturn(Optional.of(id));
    when(users.findById(id))
        .thenReturn(
            Optional.of(user(id, UserStatus.PENDING_VERIFICATION, false)),
            Optional.of(user(id, UserStatus.ACTIVE, false)));
    when(accounts.link(eq(id), any(), anyString(), anyString())).thenReturn(true);
    session(id);

    complete.complete("github", "c", "s");

    verify(accounts).claimPendingAccount(id);
  }

  @Test
  void anAdminCreatedPendingAccountIsNotClaimed() {
    verified();
    when(accounts.findLinkedUser(any(), anyString())).thenReturn(Optional.empty());
    when(accounts.lockUserByEmail(anyString())).thenReturn(Optional.of(id));
    when(users.findById(id))
        .thenReturn(Optional.of(user(id, UserStatus.PENDING_VERIFICATION, true)));
    when(accounts.link(eq(id), any(), anyString(), anyString())).thenReturn(true);
    session(id);

    complete.complete("github", "c", "s");

    verify(accounts, never()).claimPendingAccount(any());
  }

  @Test
  void aLostRaceOnTheLinkFallsBackToTheWinnersAccount() {
    verified();
    UUID winner = UUID.randomUUID();
    when(accounts.findLinkedUser(any(), anyString()))
        .thenReturn(Optional.empty(), Optional.of(winner));
    when(accounts.lockUserByEmail(anyString())).thenReturn(Optional.of(id));
    when(users.findById(any()))
        .thenReturn(Optional.of(user(id, UserStatus.ACTIVE, false)))
        .thenReturn(Optional.of(user(winner, UserStatus.ACTIVE, false)));
    when(accounts.link(eq(id), any(), anyString(), anyString())).thenReturn(false);
    session(winner);

    complete.complete("github", "c", "s");

    verify(users).findById(winner);
  }

  @Test
  void aShortProviderNameFallsBackToTheEmailLocalPart() {
    when(states.consume("s")).thenReturn(true);
    when(providers.fetchProfile(any(), anyString()))
        .thenReturn(new OAuthProfile(OAuthProvider.GOOGLE, "g-1", "binh@example.test", true, "B"));
    when(accounts.findLinkedUser(any(), anyString())).thenReturn(Optional.empty());
    when(accounts.lockUserByEmail(anyString())).thenReturn(Optional.empty());
    when(accounts.createStudent("binh@example.test", "binh")).thenReturn(Optional.of(id));
    when(accounts.link(eq(id), any(), anyString(), anyString())).thenReturn(true);
    when(users.findById(id)).thenReturn(Optional.of(user(id, UserStatus.ACTIVE, false)));
    session(id);

    complete.complete("google", "c", "s");

    verify(accounts).createStudent("binh@example.test", "binh");
  }

  @Test
  void anUnknownProviderInTheCallbackIs400() {
    assertThatThrownBy(() -> complete.complete("facebook", "c", "s"))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(code(e)).isEqualTo("IDT-V28"));
  }
}
