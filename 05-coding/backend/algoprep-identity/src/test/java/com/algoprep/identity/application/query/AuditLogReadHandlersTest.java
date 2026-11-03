package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.AuditExport;
import com.algoprep.identity.application.dto.AuditLogPageResult;
import com.algoprep.identity.application.dto.AuditStreamBatch;
import com.algoprep.identity.application.dto.AuditStreamRequest;
import com.algoprep.identity.application.dto.CategoryCount;
import com.algoprep.identity.application.dto.ExportAuditLogsQuery;
import com.algoprep.identity.application.dto.SearchAuditLogsQuery;
import com.algoprep.identity.domain.model.ActiveAdmin;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogFilter;
import com.algoprep.identity.domain.model.AuditLogRow;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** #39, #40, #64, #65, #66: the five read handlers over a mocked repository. */
class AuditLogReadHandlersTest {
  private static final Instant NOW = Instant.parse("2026-11-01T05:00:00Z");

  private final AuditLogReadRepository logs = mock(AuditLogReadRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final AuditLogFilterParser parser = new AuditLogFilterParser(logs, clock);
  private final UUID admin = UUID.randomUUID();

  AuditLogReadHandlersTest() {
    when(clock.now()).thenReturn(NOW);
    when(logs.retentionDays()).thenReturn(90);
  }

  private static AuditLogRow row(int n, String action, String after) {
    return new AuditLogRow(
        new UUID(0, n),
        NOW.minusSeconds(n),
        "Admin " + n,
        "admin" + n + "@example.test",
        action,
        "USER",
        "target-" + n,
        null,
        after);
  }

  private static List<AuditLogRow> rows(int count) {
    return IntStream.rangeClosed(1, count).mapToObj(n -> row(n, "PASSWORD_CHANGED", null)).toList();
  }

  private SearchSystemAuditLogsQueryHandler searchHandler() {
    return new SearchSystemAuditLogsQueryHandler(parser, logs);
  }

  private static SearchAuditLogsQuery search(String q, String cursor) {
    return new SearchAuditLogsQuery(q, null, null, null, cursor);
  }

  // ---- #39 -----------------------------------------------------------------------------------

  @Test
  void anEmptyLogGivesNoItemsAndNoCursor() {
    when(logs.search(any(), any(), eq(51))).thenReturn(List.of());

    AuditLogPageResult page = searchHandler().search(search(null, null));

    assertThat(page.items()).isEmpty();
    assertThat(page.nextCursor()).isNull();
  }

  @Test
  void exactlyFiftyRowsIsTheLastPage() {
    when(logs.search(any(), any(), eq(51))).thenReturn(rows(50));

    AuditLogPageResult page = searchHandler().search(search(null, null));

    assertThat(page.items()).hasSize(50);
    assertThat(page.nextCursor()).isNull();
  }

  @Test
  void fiftyOneRowsGiveFiftyItemsAndACursorPointingAtTheFiftieth() {
    List<AuditLogRow> fetched = rows(51);
    when(logs.search(any(), any(), eq(51))).thenReturn(fetched);

    AuditLogPageResult page = searchHandler().search(search(null, null));

    assertThat(page.items()).hasSize(50);
    AuditLogCursor decoded = AuditCursorCodec.decode(page.nextCursor(), AuditLogFilter.none());
    assertThat(decoded)
        .isEqualTo(new AuditLogCursor(fetched.get(49).createdAt(), fetched.get(49).id()));
  }

  @Test
  void theCursorIsPassedToTheRepositoryAndItemsAreMaskedAndClassified() {
    AuditLogCursor after = new AuditLogCursor(NOW, new UUID(1, 1));
    String token = AuditCursorCodec.encode(after, AuditLogFilter.none());
    when(logs.search(any(), eq(after), eq(51)))
        .thenReturn(List.of(row(1, "ROLE_DELETED", "{\"name\":\"R\",\"token\":\"leak\"}")));

    AuditLogPageResult page = searchHandler().search(search(null, token));

    assertThat(page.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.categoryCode()).isEqualTo("PERMISSION");
              assertThat(item.serviceCode()).isEqualTo("permission");
              assertThat(item.safeParams())
                  .containsEntry("name", "R")
                  .containsEntry("token", "***");
            });
  }

  @Test
  void aBadFilterIsRejectedBeforeTheRepositoryIsRead() {
    assertThatThrownBy(() -> searchHandler().search(search("a".repeat(101), null)))
        .satisfies(e -> assertCode(e, "IDT-444"));
    assertThatThrownBy(() -> searchHandler().search(search(null, "garbage")))
        .satisfies(e -> assertCode(e, "IDT-443"));

    verify(logs, never()).search(any(), any(), anyInt());
  }

  // ---- #40 -----------------------------------------------------------------------------------

  private ExportSystemAuditLogsQueryHandler exportHandler() {
    return new ExportSystemAuditLogsQueryHandler(parser, logs, audit);
  }

  private ExportAuditLogsQuery export(List<String> columns, String language) {
    return new ExportAuditLogsQuery(admin, "x", "AUTH", null, null, columns, "csv", language);
  }

  private static List<List<String>> drain(AuditExport export) {
    List<List<String>> out = new ArrayList<>();
    export.rows().forEachRemaining(out::add);
    return out;
  }

  @Test
  void exportKeepsTheRequestedColumnOrderAndRendersOnlyThem() {
    when(logs.search(any(), any(), eq(500))).thenReturn(List.of(row(1, "PASSWORD_CHANGED", null)));

    AuditExport result = exportHandler().open(export(List.of("id", "actor", "category"), null));

    assertThat(result.headers()).containsExactly("Mã sự kiện", "Người thực hiện", "Phân loại");
    assertThat(drain(result))
        .containsExactly(List.of(new UUID(0, 1).toString(), "admin1@example.test", "AUTH"));
  }

  @Test
  void contentIsASentenceInTheRequestedLanguageWithMaskedParams() {
    when(logs.search(any(), any(), eq(500)))
        .thenReturn(List.of(row(1, "PASSWORD_CHANGED", "{\"newPassword\":\"p\",\"by\":\"self\"}")));

    List<String> vi =
        drain(exportHandler().open(export(List.of("content"), "vi-VN,vi;q=0.9"))).get(0);
    List<String> en = drain(exportHandler().open(export(List.of("content"), "en-US"))).get(0);

    assertThat(vi).containsExactly("Người dùng đổi mật khẩu (newPassword=***; by=self)");
    assertThat(en).containsExactly("User changed the password (newPassword=***; by=self)");
  }

  @Test
  void anActionWithoutATemplateGetsAGenericSentence() {
    when(logs.search(any(), any(), eq(500))).thenReturn(List.of(row(1, "ROLE_DELETED", null)));

    assertThat(drain(exportHandler().open(export(List.of("content"), "en"))).get(0))
        .containsExactly("Action ROLE_DELETED on USER target-1");
  }

  @Test
  void theExportIsRecordedBeforeAnyRowIsRead() {
    exportHandler().open(export(List.of("time", "id"), null));

    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actorUserId()).isEqualTo(admin);
    assertThat(entry.getValue().actionType()).isEqualTo("AUDIT_LOG_EXPORTED");
    assertThat(entry.getValue().afterJson()).contains("\"columns\":[\"time\",\"id\"]");
    // Opening only validates and records; rows are read lazily by the iterator.
    verify(logs, never()).search(any(), any(), anyInt());
  }

  @Test
  void unknownEmptyOrRepeatedColumnsAndOtherFormatsAreIdt443AndNothingIsRecorded() {
    List<List<String>> bad =
        List.of(List.of("nope"), List.of(), List.of("id", "id"), List.of("time", "password_hash"));
    for (List<String> columns : bad) {
      assertThatThrownBy(() -> exportHandler().open(export(columns, null)))
          .satisfies(e -> assertCode(e, "IDT-443"));
    }
    assertThatThrownBy(() -> exportHandler().open(export(null, null)))
        .satisfies(e -> assertCode(e, "IDT-443"));
    assertThatThrownBy(
            () ->
                exportHandler()
                    .open(
                        new ExportAuditLogsQuery(
                            admin, null, null, null, null, List.of("id"), "xlsx", null)))
        .satisfies(e -> assertCode(e, "IDT-443"));

    verify(audit, never()).record(any());
  }

  @Test
  void aLongKeywordOnExportIsIdt444() {
    ExportAuditLogsQuery tooLong =
        new ExportAuditLogsQuery(
            admin, "a".repeat(101), null, null, null, List.of("id"), "csv", null);

    assertThatThrownBy(() -> exportHandler().open(tooLong))
        .satisfies(e -> assertCode(e, "IDT-444"));
    verify(audit, never()).record(any());
  }

  @Test
  void exportReadsPageByPageUsingTheLastRowAsTheNextCursor() {
    List<AuditLogRow> first = rows(500);
    AuditLogCursor afterFirst = new AuditLogCursor(first.get(499).createdAt(), first.get(499).id());
    when(logs.search(any(), eq(null), eq(500))).thenReturn(first);
    when(logs.search(any(), eq(afterFirst), eq(500))).thenReturn(rows(3));

    AuditExport result = exportHandler().open(export(List.of("id"), null));

    assertThat(drain(result)).hasSize(503);
    verify(logs).search(any(), eq(afterFirst), eq(500));
  }

  // ---- #64, #65 ------------------------------------------------------------------------------

  @Test
  void activeAdminsPassThroughTheRepositoryRows() {
    ActiveAdmin a = new ActiveAdmin(admin, "Admin", "Quản trị viên", 3, NOW);
    when(logs.activeAdmins()).thenReturn(List.of(a));

    assertThat(new ListActiveAdminsQueryHandler(logs).list()).containsExactly(a);
  }

  @Test
  void breakdownAlwaysHasFourGroupsInOrderAndFoldsActionTypes() {
    when(logs.actionCountsLastSevenDays())
        .thenReturn(
            Map.of(
                "PASSWORD_CHANGED", 2L,
                "EMAIL_CHANGED", 1L,
                "ROLE_DELETED", 4L,
                "PROBLEM_PUBLISHED", 5L,
                "UNKNOWN_THING", 1L));

    assertThat(new GetAuditCategoryBreakdownQueryHandler(logs).get())
        .containsExactly(
            new CategoryCount("AUTH", 3),
            new CategoryCount("PERMISSION", 4),
            new CategoryCount("CONFIG", 1),
            new CategoryCount("CONTENT", 5));
  }

  @Test
  void breakdownOfAnEmptyLogIsFourZeros() {
    when(logs.actionCountsLastSevenDays()).thenReturn(Map.of());

    assertThat(new GetAuditCategoryBreakdownQueryHandler(logs).get())
        .containsExactly(
            new CategoryCount("AUTH", 0),
            new CategoryCount("PERMISSION", 0),
            new CategoryCount("CONFIG", 0),
            new CategoryCount("CONTENT", 0));
  }

  // ---- #66 -----------------------------------------------------------------------------------

  private final StreamSystemAuditLogQueryHandler stream =
      new StreamSystemAuditLogQueryHandler(logs);

  @Test
  void withNoStartingPointItAnswersTheNewestPositionOnly() {
    AuditLogCursor newest = new AuditLogCursor(NOW, new UUID(9, 9));
    when(logs.latest()).thenReturn(Optional.of(newest));

    AuditStreamBatch batch = stream.next(new AuditStreamRequest(null, null, null, 1));

    assertThat(batch.items()).isEmpty();
    assertThat(batch.position()).isEqualTo(newest);
    assertThat(batch.resync()).isFalse();
  }

  @Test
  void anEmptyLogHasNoPosition() {
    when(logs.latest()).thenReturn(Optional.empty());

    assertThat(stream.next(new AuditStreamRequest(null, null, null, 1)).position()).isNull();
  }

  @Test
  void rowsAfterAPositionAreMaskedAndTheBatchPositionIsTheLastRow() {
    AuditLogCursor start = new AuditLogCursor(NOW.minusSeconds(10), new UUID(0, 0));
    List<AuditLogRow> fresh =
        List.of(row(2, "PASSWORD_CHANGED", "{\"token\":\"t\"}"), row(1, "EMAIL_CHANGED", null));
    when(logs.newer(start, null, 100)).thenReturn(fresh);

    AuditStreamBatch batch = stream.next(new AuditStreamRequest(start, null, null, 100));

    assertThat(batch.items()).hasSize(2);
    assertThat(batch.items().get(0).safeParams()).containsEntry("token", "***");
    assertThat(batch.position())
        .isEqualTo(new AuditLogCursor(fresh.get(1).createdAt(), fresh.get(1).id()));
  }

  @Test
  void aQuietPollKeepsThePosition() {
    AuditLogCursor start = new AuditLogCursor(NOW, new UUID(1, 2));
    when(logs.newer(start, null, 100)).thenReturn(List.of());

    assertThat(stream.next(new AuditStreamRequest(start, null, null, 100)).position())
        .isEqualTo(start);
  }

  @Test
  void aKnownLastEventIdReplaysWhatWasMissedUpToTheBound() {
    UUID marker = new UUID(5, 5);
    AuditLogCursor markerAt = new AuditLogCursor(NOW.minusSeconds(60), marker);
    AuditLogCursor bound = new AuditLogCursor(NOW, new UUID(7, 7));
    when(logs.findWithinRetention(marker)).thenReturn(Optional.of(markerAt));
    when(logs.newer(markerAt, bound, 200)).thenReturn(List.of(row(1, "PASSWORD_CHANGED", null)));

    AuditStreamBatch batch = stream.next(new AuditStreamRequest(null, marker, bound, 200));

    assertThat(batch.resync()).isFalse();
    assertThat(batch.items()).hasSize(1);
  }

  @Test
  void aLastEventIdThatIsGoneOrExpiredMeansResyncNotAnError() {
    UUID marker = new UUID(5, 5);
    when(logs.findWithinRetention(marker)).thenReturn(Optional.empty());

    AuditStreamBatch batch = stream.next(new AuditStreamRequest(null, marker, null, 200));

    assertThat(batch.resync()).isTrue();
    assertThat(batch.items()).isEmpty();
    assertThat(batch.position()).isNull();
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }
}
