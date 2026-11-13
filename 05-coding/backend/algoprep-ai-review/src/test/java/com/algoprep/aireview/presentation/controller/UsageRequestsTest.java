package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiUsageAlertNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsageRequestsTest {
  @Test
  void rangeDaysAcceptsSevenFourteenThirtyAndDefaultsToFourteen() {
    assertThat(UsageRequests.rangeDays(null)).isEqualTo(14);
    assertThat(UsageRequests.rangeDays("7")).isEqualTo(7);
    assertThat(UsageRequests.rangeDays(" 30 ")).isEqualTo(30);
  }

  @Test
  void rangeDaysRejectsEverythingElseWithAirV19() {
    for (String bad : new String[] {"10", "0", "-7", "abc", "", "7.0", "99999999999"}) {
      assertThatThrownBy(() -> UsageRequests.rangeDays(bad))
          .isInstanceOfSatisfying(
              AiInvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-V19"));
    }
  }

  @Test
  void limitIsOneToTwentyDefaultFive() {
    assertThat(UsageRequests.limit(null)).isEqualTo(5);
    assertThat(UsageRequests.limit("1")).isEqualTo(1);
    assertThat(UsageRequests.limit("20")).isEqualTo(20);
    for (String bad : new String[] {"0", "21", "-1", "x", ""}) {
      assertThatThrownBy(() -> UsageRequests.limit(bad))
          .isInstanceOfSatisfying(
              AiInvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-V02"));
    }
  }

  @Test
  void aNonUuidAlertIdIsTheSameNotFoundAsAnUnknownOne() {
    UUID id = UUID.randomUUID();
    assertThat(UsageRequests.alertId(id.toString())).isEqualTo(id);
    assertThatThrownBy(() -> UsageRequests.alertId("not-a-uuid"))
        .isInstanceOf(AiUsageAlertNotFoundException.class);
  }
}
