package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.domain.model.AuditCategory;
import com.algoprep.identity.domain.model.AuditLogFilter;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** DD validation section 12: IDT-443 for category/date/cursor, IDT-444 for the keyword. */
class AuditLogFilterParserTest {
  // 2026-11-01 17:30 UTC is 2026-11-02 00:30 local (UTC+7), so "today" is 2026-11-02.
  private static final Instant NOW = Instant.parse("2026-11-01T17:30:00Z");
  private final AuditLogReadRepository logs = mock(AuditLogReadRepository.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final AuditLogFilterParser parser = new AuditLogFilterParser(logs, clock);

  AuditLogFilterParserTest() {
    when(clock.now()).thenReturn(NOW);
    when(logs.retentionDays()).thenReturn(90);
  }

  @Test
  void noCriteriaMeansNoFilter() {
    assertThat(parser.parse(null, null, null, null)).isEqualTo(AuditLogFilter.none());
    assertThat(parser.parse("  ", "ALL", "", " ")).isEqualTo(AuditLogFilter.none());
  }

  @Test
  void keywordIsTrimmedAndLowerCasedAndMaxIsOneHundredAfterTrim() {
    assertThat(parser.parse("  Admin@X  ", null, null, null).keyword()).isEqualTo("admin@x");
    String max = "a".repeat(100);
    assertThat(parser.parse("  " + max + "  ", null, null, null).keyword()).isEqualTo(max);

    assertThatThrownBy(() -> parser.parse("a".repeat(101), null, null, null))
        .satisfies(e -> assertCode(e, "IDT-444"));
  }

  @Test
  void categoryMustBeOneOfTheFiveCodes() {
    assertThat(parser.parse(null, "PERMISSION", null, null).category())
        .isEqualTo(AuditCategory.PERMISSION);

    for (String bad : new String[] {"auth", "USER", "ALL "}) {
      assertThatThrownBy(() -> parser.parse(null, bad, null, null))
          .satisfies(e -> assertCode(e, "IDT-443"));
    }
  }

  @Test
  void aDayRangeIsClosedAtBothEndsInLocalTime() {
    AuditLogFilter filter = parser.parse(null, null, "2026-10-30", "2026-11-01");

    // 2026-10-30 00:00 +07:00 is 2026-10-29 17:00 UTC; the end is the start of 2026-11-02 local.
    assertThat(filter.from()).isEqualTo(Instant.parse("2026-10-29T17:00:00Z"));
    assertThat(filter.toExclusive()).isEqualTo(Instant.parse("2026-11-01T17:00:00Z"));
  }

  @Test
  void theSameDayAndTodayAreAccepted() {
    assertThat(parser.parse(null, null, "2026-11-02", "2026-11-02").from()).isNotNull();
  }

  @Test
  void invalidDatesAreRejected() {
    String[][] cases = {
      {"2026-13-01", null},
      {"2026-02-30", null},
      {"01/11/2026", null},
      {null, "tomorrow"},
      {"2026-11-03", null},
      {null, "2026-11-03"},
      {"2026-11-01", "2026-10-31"}
    };
    for (String[] c : cases) {
      assertThatThrownBy(() -> parser.parse(null, null, c[0], c[1]))
          .satisfies(e -> assertCode(e, "IDT-443"));
    }
  }

  @Test
  void aStartOlderThanTheRetentionWindowIsRejectedButTheEdgeIsAccepted() {
    // today 2026-11-02 minus 90 days is 2026-08-04.
    assertThat(parser.parse(null, null, "2026-08-04", null).from()).isNotNull();

    assertThatThrownBy(() -> parser.parse(null, null, "2026-08-03", null))
        .satisfies(e -> assertCode(e, "IDT-443"));
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }
}
