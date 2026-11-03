package com.algoprep.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Exact grace-period boundary, lifecycle source and anonymization guards. */
class RestorableAccountTest {
  private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

  @ParameterizedTest
  @ValueSource(longs = {-1, 0, 1})
  void exact30DayBoundaryIsExclusive(long nanos) {
    RestorableAccount account =
        new RestorableAccount(
            UUID.randomUUID(),
            UserStatus.DEACTIVATED,
            "SELF",
            NOW.minus(Duration.ofDays(30)).plusNanos(nanos),
            null,
            NOW);
    assertThat(account.canRestore()).isEqualTo(nanos > 0);
  }

  @ParameterizedTest
  @CsvSource({
    "SELF,DEACTIVATED,0,false,true",
    "ADMIN,DEACTIVATED,0,false,false",
    "SELF,ACTIVE,0,false,false",
    "SELF,PENDING_VERIFICATION,0,false,false",
    "SELF,DEACTIVATED,0,true,false",
    "SELF,DEACTIVATED,31,false,false",
    "SELF,DEACTIVATED,-1,false,false"
  })
  void onlySelfDeactivatedUnanonymizedAccountsInGraceAreEligible(
      String source, UserStatus status, int days, boolean anonymized, boolean expected) {
    RestorableAccount account =
        new RestorableAccount(
            UUID.randomUUID(),
            status,
            source,
            NOW.minus(Duration.ofDays(days)),
            anonymized ? NOW : null,
            NOW);
    assertThat(account.canRestore()).isEqualTo(expected);
  }

  @Test
  void missingSourceOrDeletionTimeCannotBeRestored() {
    assertThat(
            new RestorableAccount(UUID.randomUUID(), UserStatus.DEACTIVATED, null, NOW, null, NOW)
                .canRestore())
        .isFalse();
    assertThat(
            new RestorableAccount(
                    UUID.randomUUID(), UserStatus.DEACTIVATED, "SELF", null, null, NOW)
                .canRestore())
        .isFalse();
  }
}
