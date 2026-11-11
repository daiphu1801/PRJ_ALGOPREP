package com.algoprep.aireview.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.AnomalyRunOutcome;
import com.algoprep.aireview.application.ports.out.UserDirectoryPort;
import com.algoprep.aireview.domain.exception.AiUsageAlertNotFoundException;
import com.algoprep.aireview.domain.model.AlertReview;
import com.algoprep.aireview.domain.model.AnomalyAlert;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.ports.out.AiUsageRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiUsageHandlersTest {
  private static final Instant NOW = Instant.parse("2026-11-10T05:00:00Z");
  private final AiUsageRepository usage = mock(AiUsageRepository.class);
  private final UserDirectoryPort users = mock(UserDirectoryPort.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final DetectAiUsageAnomaliesCommandHandler detect =
      new DetectAiUsageAnomaliesCommandHandler(usage, users, clock, new BigDecimal("5"));
  private final MarkAiUsageAlertReviewedCommandHandler mark =
      new MarkAiUsageAlertReviewedCommandHandler(usage);

  /** One heavy user (25 calls) and five users with one call: average 5.00, ratio exactly 5. */
  private static Map<UUID, Integer> population(UUID heavy) {
    Map<UUID, Integer> counts = new HashMap<>();
    counts.put(heavy, 25);
    for (int i = 0; i < 5; i++) {
      counts.put(UUID.randomUUID(), 1);
    }
    return counts;
  }

  private void lockedIn(Map<UUID, Integer> counts) {
    when(clock.now()).thenReturn(NOW);
    when(usage.tryAnomalyLock()).thenReturn(true);
    when(usage.callCountsSince(NOW.minusSeconds(86400))).thenReturn(counts);
    when(usage.usersWithOpenAlert()).thenReturn(Set.of());
  }

  @Test
  void markingReturnsTheStoredReview() {
    UUID id = UUID.randomUUID();
    AlertReview review = new AlertReview(id, NOW, false);
    when(usage.markReviewed(id)).thenReturn(Optional.of(review));
    assertThat(mark.review(id)).isSameAs(review);
  }

  @Test
  void markingAnUnknownAlertIsAir120() {
    UUID id = UUID.randomUUID();
    when(usage.markReviewed(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> mark.review(id)).isInstanceOf(AiUsageAlertNotFoundException.class);
  }

  @Test
  void anotherInstanceHoldingTheLockSkipsTheRun() {
    when(usage.tryAnomalyLock()).thenReturn(false);
    assertThat(detect.detect()).isEqualTo(new AnomalyRunOutcome(true, 0));
    verify(usage, never()).callCountsSince(any());
  }

  @Test
  void aFlaggedStudentGetsExactlyOneOpenAlertWithTheWindow() {
    UUID heavy = UUID.randomUUID();
    lockedIn(population(heavy));
    when(users.getBaseCategories(any())).thenReturn(Map.of(heavy, CallerCategory.STUDENT));
    assertThat(detect.detect()).isEqualTo(new AnomalyRunOutcome(false, 1));
    ArgumentCaptor<AnomalyAlert> saved = ArgumentCaptor.forClass(AnomalyAlert.class);
    verify(usage).insertAlert(saved.capture());
    assertThat(saved.getValue().userId()).isEqualTo(heavy);
    assertThat(saved.getValue().windowStart()).isEqualTo(NOW.minusSeconds(86400));
    assertThat(saved.getValue().windowEnd()).isEqualTo(NOW);
    assertThat(saved.getValue().callCount()).isEqualTo(25);
  }

  @Test
  void nobodyAboveTheThresholdWritesNothingAndAsksNoRoles() {
    lockedIn(Map.of(UUID.randomUUID(), 3, UUID.randomUUID(), 3));
    assertThat(detect.detect()).isEqualTo(new AnomalyRunOutcome(false, 0));
    verify(usage, never()).insertAlert(any());
    verify(users, never()).getBaseCategories(any());
  }

  @Test
  void anAdminIsCountedInTheAverageButNeverAlerted() {
    UUID heavy = UUID.randomUUID();
    lockedIn(population(heavy));
    when(users.getBaseCategories(any())).thenReturn(Map.of(heavy, CallerCategory.ADMIN));
    assertThat(detect.detect().created()).isZero();
    verify(usage, never()).insertAlert(any());
  }

  @Test
  void anUnknownRoleIsNotGuessed() {
    UUID heavy = UUID.randomUUID();
    lockedIn(population(heavy));
    when(users.getBaseCategories(any())).thenReturn(Map.of());
    assertThat(detect.detect().created()).isZero();
  }

  @Test
  void anAccountWithAnOpenAlertGetsNoSecondOne() {
    UUID heavy = UUID.randomUUID();
    lockedIn(population(heavy));
    when(usage.usersWithOpenAlert()).thenReturn(Set.of(heavy));
    assertThat(detect.detect().created()).isZero();
    verify(users, never()).getBaseCategories(any());
    verify(usage, never()).insertAlert(any());
  }

  @Test
  void aFailingRoleLookupPropagatesSoNothingIsGuessed() {
    UUID heavy = UUID.randomUUID();
    lockedIn(population(heavy));
    when(users.getBaseCategories(any())).thenThrow(new IllegalStateException("down"));
    assertThatThrownBy(detect::detect).isInstanceOf(IllegalStateException.class);
    verify(usage, never()).insertAlert(any());
  }
}
