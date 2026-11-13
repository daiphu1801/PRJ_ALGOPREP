package com.algoprep.aireview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BudgetForecastTest {
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

  @Test
  void theUtcPlus7DayAndMonthFollowTheLocalCalendar() {
    Instant lastMomentUtc = Instant.parse("2026-11-30T16:59:59Z");
    assertThat(BudgetPolicy.dayOf(lastMomentUtc)).isEqualTo(LocalDate.of(2026, 11, 30));
    assertThat(BudgetPolicy.monthStartOf(lastMomentUtc)).isEqualTo(LocalDate.of(2026, 11, 1));
    Instant nextMonth = Instant.parse("2026-11-30T17:00:00Z");
    assertThat(BudgetPolicy.dayOf(nextMonth)).isEqualTo(LocalDate.of(2026, 12, 1));
    assertThat(BudgetPolicy.monthStartOf(nextMonth)).isEqualTo(LocalDate.of(2026, 12, 1));
  }

  @Test
  void forecastRoundsTheRemainingDaysUp() {
    // 100 tokens over 7 days, 10 remaining: 10 / (100 / 7) = 0.7 days, rounded up to 1.
    assertThat(BudgetPolicy.projectedRunOutDate(10, 100, 7, TODAY)).isEqualTo(TODAY.plusDays(1));
    // Exactly divisible: 700 remaining at 100 per day is 7 days.
    assertThat(BudgetPolicy.projectedRunOutDate(700, 700, 7, TODAY)).isEqualTo(TODAY.plusDays(7));
  }

  @Test
  void noForecastWhenTheWindowHasNoUsage() {
    assertThat(BudgetPolicy.projectedRunOutDate(990, 0, 7, TODAY)).isNull();
  }

  @Test
  void aZeroDayWindowIsTreatedAsOneDay() {
    assertThat(BudgetPolicy.projectedRunOutDate(1000, 500, 0, TODAY)).isEqualTo(TODAY.plusDays(2));
  }
}
