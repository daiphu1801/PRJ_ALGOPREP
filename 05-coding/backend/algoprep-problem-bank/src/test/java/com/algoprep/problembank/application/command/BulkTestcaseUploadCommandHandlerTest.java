package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.BulkTestcaseUpload.Confirm;
import com.algoprep.problembank.application.dto.BulkTestcaseUpload.Item;
import com.algoprep.problembank.application.dto.BulkTestcaseUpload.RequestUrls;
import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import com.algoprep.problembank.application.dto.TestcaseUploadSession.Session;
import com.algoprep.problembank.application.policy.TestcaseViewMapper;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.ProblemListVersionPort;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.ports.out.TestcaseUploadSessionPort;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.ApprovalCounts;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.model.ProblemTouch;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.ports.out.ProblemContentRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class BulkTestcaseUploadCommandHandlerTest {
  private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
  private static final Instant STAMP = Instant.parse("2026-11-06T10:05:00Z");

  private final ProblemContentRepository content = mock(ProblemContentRepository.class);
  private final TestcaseWriteRepository testcases = mock(TestcaseWriteRepository.class);
  private final PublishTestcaseSnapshotUseCase snapshots =
      mock(PublishTestcaseSnapshotUseCase.class);
  private final TestcaseViewMapper views = mock(TestcaseViewMapper.class);
  private final TestcaseObjectStorePort store = mock(TestcaseObjectStorePort.class);
  private final FakeSessions sessions = new FakeSessions();
  private final AtomicLong counter = new AtomicLong();
  private final IdGeneratorPort ids = () -> new UUID(0, counter.incrementAndGet());
  private final ClockPort clock = () -> NOW;
  private final UUID actor = UUID.randomUUID();
  private final UUID problemId = UUID.randomUUID();
  private BulkTestcaseUploadCommandHandler handler;
  private String status = "UNPUBLISHED";
  private UUID author = actor;

  /** Map-backed session store; {@code broken} makes every call fail like a Redis outage. */
  private static final class FakeSessions implements TestcaseUploadSessionPort {
    final Map<UUID, Session> data = new HashMap<>();
    boolean broken;

    @Override
    public void save(Session session, Duration ttl) {
      fail();
      data.put(session.id(), session);
    }

    @Override
    public Optional<Session> find(UUID uploadId) {
      fail();
      return Optional.ofNullable(data.get(uploadId));
    }

    @Override
    public void update(Session session) {
      fail();
      data.put(session.id(), session);
    }

    private void fail() {
      if (broken) {
        throw new IllegalStateException("redis down");
      }
    }
  }

  @BeforeEach
  void setup() {
    TransactionSynchronizationManager.initSynchronization();
    when(content.lock(problemId))
        .thenAnswer(
            call ->
                Optional.of(
                    new ProblemContentSnapshot(
                        problemId, "CODE", author, status, ContentTestData.valid(), NOW)));
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(3, 3, 1));
    when(testcases.nextPosition(problemId)).thenReturn(4);
    when(testcases.touch(problemId, actor)).thenReturn(new ProblemTouch(STAMP, 7));
    when(store.presignPut(anyString(), any())).thenAnswer(c -> "http://minio/" + c.getArgument(0));
    when(views.view(any()))
        .thenAnswer(
            c -> {
              var t =
                  (com.algoprep.problembank.domain.model.AuthoringProblemRecord.AuthoringTestcase)
                      c.getArgument(0);
              return new TestcaseView(
                  t.id(),
                  t.position(),
                  t.visibility(),
                  t.origin(),
                  t.approved(),
                  t.category(),
                  t.inputInline(),
                  t.inputStorage(),
                  0,
                  null,
                  t.expectedInline(),
                  t.expectedStorage(),
                  0,
                  null);
            });
    var support =
        new TestcaseAuthoringSupport(
            content, testcases, snapshots, mock(ProblemListVersionPort.class), views);
    var objects = new BulkUploadObjects(store, new TestcaseContentStore(store, ids));
    handler = new BulkTestcaseUploadCommandHandler(support, testcases, objects, sessions, clock);
  }

  @AfterEach
  void cleanup() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private RequestUrls request(Item... items) {
    return new RequestUrls(new AuthoringScope(actor, false), problemId, List.of(items));
  }

  private static Item pair(String key, long in, long out) {
    return new Item(key, in, out, "HIDDEN", null);
  }

  private String code(Runnable action) {
    return ((AlgoPrepException) catchThrowable(action)).errorCode();
  }

  private static Throwable catchThrowable(Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      return e;
    }
    throw new AssertionError("expected a failure");
  }

  // ---- request URLs (#45)

  @Test
  void signsAPutUrlPerSideForServerMadeKeysAndKeepsASessionWithoutWritingRows() {
    var plan =
        handler.requestUrls(
            request(
                pair("a", 10, 20), new Item("b", 30, 40, "SAMPLE", "EXTREME"), pair("c", 5, 6)));

    assertThat(plan.items()).hasSize(3);
    assertThat(plan.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    var first = plan.items().get(0);
    assertThat(first.input().putUrl())
        .contains("problems/" + problemId + "/testcases/" + first.testcaseId() + "/input-");
    assertThat(first.expectedOutput().putUrl()).contains("/expected_output-");
    var saved = sessions.data.get(plan.uploadId());
    assertThat(saved.problemId()).isEqualTo(problemId);
    assertThat(saved.userId()).isEqualTo(actor);
    assertThat(saved.confirmed()).isFalse();
    assertThat(saved.slots().get(1).visibility()).isEqualTo("SAMPLE");
    assertThat(saved.slots().get(0).visibility()).isEqualTo("HIDDEN");
    verify(store)
        .presignPut(
            eq(first.input().putUrl().substring("http://minio/".length())),
            eq(Duration.ofMinutes(15)));
    verify(testcases, never()).insert(any(), any());
  }

  @Test
  void aMissingVisibilityMeansHidden() {
    var plan = handler.requestUrls(request(new Item("a", 1, 1, null, null)));

    assertThat(sessions.data.get(plan.uploadId()).slots().get(0).visibility()).isEqualTo("HIDDEN");
  }

  @Test
  void badItemsAreRefusedWithPrb001() {
    var longKey = "k".repeat(101);
    var cases =
        List.of(
            new RequestUrls(new AuthoringScope(actor, false), problemId, null),
            new RequestUrls(new AuthoringScope(actor, false), problemId, List.of()),
            request(pair("a", 1, 1), pair("a", 1, 1)),
            request(pair(" ", 1, 1)),
            request(pair(longKey, 1, 1)),
            request(pair("a", 0, 1)),
            request(pair("a", 1, -1)),
            request(new Item("a", 1, 1, "X", null)),
            request(new Item("a", 1, 1, "HIDDEN", "X")),
            request(new Item(null, 1, 1, "HIDDEN", null)));
    for (var bad : cases) {
      assertThat(code(() -> handler.requestUrls(bad))).isEqualTo("PRB-001");
    }
    assertThat(sessions.data).isEmpty();
  }

  @Test
  void aHundredPairsPassAndAHundredAndOneAreTooMuch() {
    var items = new ArrayList<Item>();
    for (int i = 0; i < 100; i++) {
      items.add(pair("k" + i, 1, 1));
    }
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(0, 0, 0));
    assertThat(
            handler
                .requestUrls(new RequestUrls(new AuthoringScope(actor, false), problemId, items))
                .items())
        .hasSize(100);

    items.add(pair("extra", 1, 1));
    assertThat(
            code(
                () ->
                    handler.requestUrls(
                        new RequestUrls(new AuthoringScope(actor, false), problemId, items))))
        .isEqualTo("PRB-047");
  }

  @Test
  void aSideAndTheTotalAreCappedAt64And256Megabytes() {
    long mb = 1024L * 1024;

    assertThat(handler.requestUrls(request(pair("a", 64 * mb, 64 * mb))).items()).hasSize(1);
    assertThat(code(() -> handler.requestUrls(request(pair("a", 64 * mb + 1, 1)))))
        .isEqualTo("PRB-047");
    assertThat(code(() -> handler.requestUrls(request(pair("a", 1, 64 * mb + 1)))))
        .isEqualTo("PRB-047");
    assertThat(
            handler
                .requestUrls(request(pair("a", 64 * mb, 64 * mb), pair("b", 64 * mb, 64 * mb)))
                .items())
        .hasSize(2);
    assertThat(
            code(
                () ->
                    handler.requestUrls(
                        request(
                            pair("a", 64 * mb, 64 * mb),
                            pair("b", 64 * mb, 64 * mb),
                            pair("c", 1, 1)))))
        .isEqualTo("PRB-047");
  }

  @Test
  void aProblemHoldsAtMostFiveHundredCasesAfterTheBatch() {
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(499, 499, 2));

    assertThat(code(() -> handler.requestUrls(request(pair("a", 1, 1), pair("b", 1, 1)))))
        .isEqualTo("PRB-046");
    assertThat(handler.requestUrls(request(pair("a", 1, 1))).items()).hasSize(1);
  }

  @Test
  void anotherAuthorsDraftIs404AndTheirPublishedProblemIs403() {
    author = UUID.randomUUID();

    assertThat(code(() -> handler.requestUrls(request(pair("a", 1, 1))))).isEqualTo("PRB-010");
    status = "PUBLISHED";
    assertThat(code(() -> handler.requestUrls(request(pair("a", 1, 1))))).isEqualTo("PRB-003");
    assertThat(sessions.data).isEmpty();
  }

  @Test
  void anAdminMayUploadForSomeoneElsesProblem() {
    author = UUID.randomUUID();
    var admin = UUID.randomUUID();

    var plan =
        handler.requestUrls(
            new RequestUrls(new AuthoringScope(admin, true), problemId, List.of(pair("a", 1, 1))));

    assertThat(sessions.data.get(plan.uploadId()).userId()).isEqualTo(admin);
  }

  @Test
  void aSigningFailureAndASessionFailureAre503Prb052() {
    when(store.presignPut(anyString(), any())).thenThrow(new TestcaseStorageUnavailableException());
    assertThat(code(() -> handler.requestUrls(request(pair("a", 1, 1))))).isEqualTo("PRB-052");

    org.mockito.Mockito.doReturn("http://minio/x").when(store).presignPut(anyString(), any());
    sessions.broken = true;
    assertThat(code(() -> handler.requestUrls(request(pair("a", 1, 1))))).isEqualTo("PRB-052");
  }

  // ---- confirm (#46)

  private Session uploaded(Item... items) {
    var plan = handler.requestUrls(request(items));
    return sessions.data.get(plan.uploadId());
  }

  private Confirm confirm(Session session) {
    return new Confirm(new AuthoringScope(actor, false), problemId, session.id());
  }

  private void objectIs(String key, byte[] bytes) {
    when(store.head(key)).thenReturn((long) bytes.length);
    when(store.get(key)).thenReturn(bytes);
  }

  private void objectSized(String key, long size) {
    when(store.head(key)).thenReturn(size);
  }

  private static byte[] text(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private void runCallbacks(int status) {
    for (TransactionSynchronization sync :
        TransactionSynchronizationManager.getSynchronizations()) {
      sync.afterCompletion(status);
    }
  }

  @Test
  void aConfirmInsertsTheWholeBatchSmallSidesInlineAndLargeSidesByKey() {
    var session = uploaded(pair("a", 3, 5), new Item("b", 9000, 2, "SAMPLE", "TYPICAL"));
    var a = session.slots().get(0);
    var b = session.slots().get(1);
    objectIs(a.input().objectKey(), text("1 2"));
    objectIs(a.expectedOutput().objectKey(), text("três"));
    objectSized(b.input().objectKey(), 9000);
    objectIs(b.expectedOutput().objectKey(), text("ok"));

    var result = handler.confirm(confirm(session));

    var captor = org.mockito.ArgumentCaptor.forClass(StoredTestcase.class);
    verify(testcases, org.mockito.Mockito.times(2)).insert(eq(problemId), captor.capture());
    var inserted = captor.getAllValues();
    assertThat(inserted).extracting(StoredTestcase::position).containsExactly(4, 5);
    assertThat(inserted).allSatisfy(t -> assertThat(t.origin()).isEqualTo("MANUAL"));
    assertThat(inserted).allSatisfy(t -> assertThat(t.approved()).isTrue());
    assertThat(inserted.get(0).input().storage()).isEqualTo("INLINE");
    assertThat(inserted.get(0).input().inline()).isEqualTo("1 2");
    assertThat(inserted.get(0).expectedOutput().inline()).isEqualTo("três");
    assertThat(inserted.get(1).input().storage()).isEqualTo("MINIO");
    assertThat(inserted.get(1).input().objectKey()).isEqualTo(b.input().objectKey());
    assertThat(inserted.get(1).visibility()).isEqualTo("SAMPLE");
    assertThat(inserted.get(1).category()).isEqualTo("TYPICAL");
    assertThat(result.added()).hasSize(2);
    assertThat(result.currentTestcaseSetVersion()).isEqualTo(7);
    assertThat(result.problemUpdatedAt()).isEqualTo(STAMP);
    assertThat(sessions.data.get(session.id()).confirmed()).isTrue();
    verify(snapshots, never()).publishSnapshot(any());

    runCallbacks(TransactionSynchronization.STATUS_COMMITTED);
    verify(store).delete(a.input().objectKey());
    verify(store).delete(a.expectedOutput().objectKey());
    verify(store).delete(b.expectedOutput().objectKey());
    verify(store, never()).delete(b.input().objectKey());
  }

  @Test
  void aPublishedProblemGetsOneSnapshotForTheWholeBatch() {
    status = "PUBLISHED";
    var session = uploaded(pair("a", 1, 1), pair("b", 1, 1));
    for (var slot : session.slots()) {
      objectIs(slot.input().objectKey(), text("i"));
      objectIs(slot.expectedOutput().objectKey(), text("o"));
    }

    handler.confirm(confirm(session));

    verify(snapshots, org.mockito.Mockito.times(1)).publishSnapshot(problemId);
  }

  @Test
  void aSecondConfirmAnswersTheFirstWithoutInsertingAgain() {
    var session = uploaded(pair("a", 1, 1));
    var slot = session.slots().get(0);
    objectIs(slot.input().objectKey(), text("i"));
    objectIs(slot.expectedOutput().objectKey(), text("o"));
    var first = handler.confirm(confirm(session));
    var stored =
        new StoredTestcase(
            slot.testcaseId(),
            "HIDDEN",
            "MANUAL",
            true,
            null,
            4,
            new com.algoprep.problembank.domain.model.StoredGradingTestcase.Content(
                "INLINE", "i", null),
            new com.algoprep.problembank.domain.model.StoredGradingTestcase.Content(
                "INLINE", "o", null));
    when(testcases.lock(problemId, slot.testcaseId())).thenReturn(Optional.of(stored));

    var second = handler.confirm(confirm(sessions.data.get(session.id())));

    assertThat(second.added()).extracting(TestcaseView::id).containsExactly(slot.testcaseId());
    assertThat(second.currentTestcaseSetVersion()).isEqualTo(first.currentTestcaseSetVersion());
    assertThat(second.problemUpdatedAt()).isEqualTo(first.problemUpdatedAt());
    verify(testcases, org.mockito.Mockito.times(1)).insert(any(), any());
  }

  @Test
  void aRepeatedConfirmSkipsRowsDeletedSinceTheFirstOne() {
    var session = uploaded(pair("a", 1, 1));
    var slot = session.slots().get(0);
    objectIs(slot.input().objectKey(), text("i"));
    objectIs(slot.expectedOutput().objectKey(), text("o"));
    handler.confirm(confirm(session));
    when(testcases.lock(problemId, slot.testcaseId())).thenReturn(Optional.empty());

    assertThat(handler.confirm(confirm(sessions.data.get(session.id()))).added()).isEmpty();
  }

  @Test
  void missingObjectsWrongSizesAndBadTextRefuseTheWholeBatchWithEachFault() {
    var session = uploaded(pair("a", 3, 3), pair("b", 3, 3), pair("c", 3, 3), pair("d", 1, 1));
    var a = session.slots().get(0);
    var b = session.slots().get(1);
    var c = session.slots().get(2);
    var d = session.slots().get(3);
    when(store.head(a.input().objectKey()))
        .thenThrow(new TestcaseStorageUnavailableException(true));
    objectIs(a.expectedOutput().objectKey(), text("abc"));
    objectSized(b.input().objectKey(), 99);
    objectIs(b.expectedOutput().objectKey(), text("abc"));
    objectIs(c.input().objectKey(), new byte[] {(byte) 0xC3, 0x28, 0x61});
    objectIs(c.expectedOutput().objectKey(), text("abc"));
    objectIs(d.input().objectKey(), text("x"));
    objectIs(d.expectedOutput().objectKey(), text("y"));

    var thrown = catchThrowable(() -> handler.confirm(confirm(session)));

    assertThat(((AlgoPrepException) thrown).errorCode()).isEqualTo("PRB-045");
    List<Object> items =
        List.copyOf((List<?>) ((ClientErrorDetails) thrown).clientErrorDetails().get("items"));
    assertThat(items)
        .containsExactly(
            Map.of("clientKey", "a", "side", "input", "problem", "MISSING"),
            Map.of("clientKey", "b", "side", "input", "problem", "SIZE_MISMATCH"),
            Map.of("clientKey", "c", "side", "input", "problem", "INVALID_TEXT"));
    verify(testcases, never()).insert(any(), any());
    assertThat(sessions.data.get(session.id()).confirmed()).isFalse();
  }

  @Test
  void aStorageOutageThatIsNotAMissingObjectIs503AndInsertsNothing() {
    var session = uploaded(pair("a", 1, 1));
    when(store.head(session.slots().get(0).input().objectKey()))
        .thenThrow(new TestcaseStorageUnavailableException());

    assertThat(code(() -> handler.confirm(confirm(session)))).isEqualTo("PRB-052");
    verify(testcases, never()).insert(any(), any());
  }

  @Test
  void aSessionThatIsUnknownForeignOrAnotherProblemsIsPrb044() {
    var session = uploaded(pair("a", 1, 1));
    var stranger =
        new Confirm(new AuthoringScope(UUID.randomUUID(), true), problemId, session.id());
    var elsewhere = new Confirm(new AuthoringScope(actor, false), problemId, UUID.randomUUID());
    sessions.data.put(
        new UUID(9, 9),
        new Session(new UUID(9, 9), UUID.randomUUID(), actor, session.slots(), null));
    var otherProblem = new Confirm(new AuthoringScope(actor, false), problemId, new UUID(9, 9));
    author = actor;

    assertThat(code(() -> handler.confirm(elsewhere))).isEqualTo("PRB-044");
    assertThat(code(() -> handler.confirm(otherProblem))).isEqualTo("PRB-044");
    assertThat(code(() -> handler.confirm(stranger))).isEqualTo("PRB-044");
    verify(testcases, never()).insert(any(), any());
  }

  @Test
  void aConfirmThatWouldPassFiveHundredIsPrb046AndASessionOutageIs503() {
    var session = uploaded(pair("a", 1, 1), pair("b", 1, 1));
    for (var slot : session.slots()) {
      objectIs(slot.input().objectKey(), text("i"));
      objectIs(slot.expectedOutput().objectKey(), text("o"));
    }
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(499, 499, 2));
    assertThat(code(() -> handler.confirm(confirm(session)))).isEqualTo("PRB-046");

    sessions.broken = true;
    assertThat(code(() -> handler.confirm(confirm(session)))).isEqualTo("PRB-052");
  }

  @Test
  void aRollbackPutsTheSessionBackToPendingSoTheConfirmCanBeRetried() {
    var session = uploaded(pair("a", 1, 1));
    var slot = session.slots().get(0);
    objectIs(slot.input().objectKey(), text("i"));
    objectIs(slot.expectedOutput().objectKey(), text("o"));
    handler.confirm(confirm(session));
    assertThat(sessions.data.get(session.id()).confirmed()).isTrue();

    runCallbacks(TransactionSynchronization.STATUS_ROLLED_BACK);

    assertThat(sessions.data.get(session.id()).confirmed()).isFalse();
    verify(store, never()).delete(anyString());
  }

  @Test
  void aRollbackWhileTheSessionStoreIsDownOnlyLogs() {
    var session = uploaded(pair("a", 1, 1));
    var slot = session.slots().get(0);
    objectIs(slot.input().objectKey(), text("i"));
    objectIs(slot.expectedOutput().objectKey(), text("o"));
    handler.confirm(confirm(session));
    sessions.broken = true;

    runCallbacks(TransactionSynchronization.STATUS_ROLLED_BACK);

    assertThat(sessions.data.get(session.id()).confirmed()).isTrue();
  }
}
