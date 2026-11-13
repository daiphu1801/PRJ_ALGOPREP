package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.InfraEventMessages;
import com.algoprep.judge.application.dto.InfraEventQuery;
import com.algoprep.judge.domain.model.InfraEvents.Position;
import com.algoprep.judge.domain.model.InfraEvents.Row;
import com.algoprep.judge.domain.model.InfraEvents.Summary;
import com.algoprep.judge.domain.ports.out.InfraEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GetInfraEventsQueryHandlerTest {
  private final InfraEventRepository repository = mock(InfraEventRepository.class);
  private final GetInfraEventsQueryHandler handler = new GetInfraEventsQueryHandler(repository);

  private Row row(String service, String kind) {
    UUID id = UUID.fromString("80000000-0000-0000-0000-000000000001");
    return new Row(
        new Position(Instant.parse("2026-11-05T00:00:00Z"), "s:" + id),
        "ERROR",
        service,
        id,
        kind,
        5);
  }

  private InfraEventQuery query() {
    return InfraEventQuery.parse(null, null, null, InfraEventQuery.dates(null, null), null);
  }

  @Test
  void pagesExposeOnlyTwentySafeEventsAndUseTheLastVisibleKey() {
    var row = row("go-judge", "SANDBOX_FAULT");
    when(repository.list(any(), any()))
        .thenReturn(IntStream.range(0, 21).mapToObj(i -> row).toList());
    var page = handler.list(query());
    assertThat(page.events()).hasSize(20);
    assertThat(page.nextCursor()).isEqualTo(InfraEventQuery.encode(row.position()));
    assertThat(page.services()).containsExactly("go-judge", "outbox");
    assertThatThrownBy(() -> page.events().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    when(repository.list(any(), any())).thenReturn(List.of(row), List.of());
    assertThat(handler.list(query()).nextCursor()).isNull();
    assertThat(handler.list(query()).events()).isEmpty();
    var summary = new Summary(2, 3, 24);
    when(repository.summary()).thenReturn(summary);
    assertThat(handler.summary()).isEqualTo(summary);
  }

  @Test
  void everyKnownReasonHasASafeApplicationMessage() {
    assertThat(InfraEventMessages.event(row("outbox", "SubmissionGraded")).message())
        .contains("SubmissionGraded", "5 lần", "80000000-0000-0000-0000-000000000001");
    for (String reason :
        List.of("WORKER_TIMEOUT", "SANDBOX_FAULT", "QUEUE_UNAVAILABLE", "SPEC_UNAVAILABLE")) {
      assertThat(InfraEventMessages.event(row("go-judge", reason)).message())
          .startsWith("Lượt nộp ");
    }
    assertThatThrownBy(() -> InfraEventMessages.event(row("go-judge", "UNKNOWN")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown infrastructure event kind");
  }

  @Test
  void filtersNormalizeAndCursorRoundTripsWithoutLosingTimestampPrecision() {
    var position = row("go-judge", "SANDBOX_FAULT").position();
    var query =
        InfraEventQuery.parse(
            "😀".repeat(101),
            "ErRoR",
            "outbox",
            InfraEventQuery.dates("2026-11-01", "2026-11-01"),
            InfraEventQuery.encode(position));
    assertThat(query.q().codePointCount(0, query.q().length())).isEqualTo(100);
    assertThat(query.level()).isEqualTo("error");
    assertThat(query.service()).isEqualTo("outbox");
    assertThat(query.cursor()).isEqualTo(position);
    assertThat(query.dates().from()).isEqualTo(Instant.parse("2026-10-31T17:00:00Z"));
    assertThat(query.dates().until()).isEqualTo(Instant.parse("2026-11-01T17:00:00Z"));
    var invalid = InfraEventQuery.parse("", "unknown", "OUTBOX", InfraEventQuery.dates("", ""), "");
    assertThat(invalid.level()).isEmpty();
    assertThat(invalid.service()).isEmpty();
    assertThat(invalid.cursor()).isNull();
  }

  @Test
  void malformedAndOversizedCursorsAndDatesAreRejectedBeforeSql() {
    for (String cursor :
        List.of("!", "a".repeat(181), "eA", "MjAyNi0xMS0wNVQwMDowMDowMFp8cy5iYWQ")) {
      assertThatThrownBy(
              () ->
                  InfraEventQuery.parse(
                      null, null, null, InfraEventQuery.dates(null, null), cursor))
          .isInstanceOf(IllegalArgumentException.class);
    }
    for (String date : List.of("2026-02-30", "bad", "2026-1-1", "0000-01-01", "+10000-01-01")) {
      assertThatThrownBy(() -> InfraEventQuery.dates(date, null))
          .isInstanceOf(RuntimeException.class);
    }
    assertThatThrownBy(() -> InfraEventQuery.dates("2026-11-02", "2026-11-01"))
        .isInstanceOf(IllegalArgumentException.class);
    var ancient =
        new Position(
            Instant.parse("0000-01-01T00:00:00Z"),
            row("go-judge", "SANDBOX_FAULT").position().sourceKey());
    assertThatThrownBy(
            () ->
                InfraEventQuery.parse(
                    null,
                    null,
                    null,
                    InfraEventQuery.dates(null, null),
                    InfraEventQuery.encode(ancient)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
