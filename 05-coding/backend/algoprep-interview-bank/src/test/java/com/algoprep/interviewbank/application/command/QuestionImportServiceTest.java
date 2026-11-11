package com.algoprep.interviewbank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.interviewbank.application.dto.ImportCommand;
import com.algoprep.interviewbank.application.dto.ImportRow;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.InterviewAuditEntry;
import com.algoprep.interviewbank.domain.model.QuestionDraft;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.AdminQuestionRepository;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import com.algoprep.interviewbank.domain.ports.out.InterviewBankAuditPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuestionImportServiceTest {
  private final AdminQuestionRepository repository = mock(AdminQuestionRepository.class);
  private final CatalogRepository catalog = mock(CatalogRepository.class);
  private final InterviewBankAuditPort audit = mock(InterviewBankAuditPort.class);
  private final QuestionImportService service =
      new QuestionImportService(
          repository, catalog, new QuestionAuthoringService(repository, audit));
  private final UUID actor = UUID.randomUUID();
  private final UUID topic = UUID.randomUUID();
  private final UUID level = UUID.randomUUID();

  @BeforeEach
  void catalogues() {
    when(catalog.list(CatalogKind.TOPIC, false))
        .thenReturn(List.of(new CatalogItem(topic, "DATABASE", "Database", 1, false, null)));
    when(catalog.list(CatalogKind.LEVEL, false))
        .thenReturn(List.of(new CatalogItem(level, "MEDIUM", "Trung bình", 1, false, null)));
    when(repository.activeTitleKeys()).thenReturn(Set.of("existing"));
    when(repository.titleKey(any())).thenAnswer(call -> call.<String>getArgument(0).toLowerCase());
    when(repository.lockReference(any(), any())).thenReturn(true);
    when(repository.insert(any(), any()))
        .thenAnswer(
            call -> {
              QuestionDraft draft = call.getArgument(0);
              return new AdminQuestion(
                  UUID.randomUUID(),
                  1,
                  draft.topicId(),
                  draft.levelId(),
                  draft.title(),
                  null,
                  List.of(),
                  draft.rubric(),
                  Instant.EPOCH,
                  Instant.EPOCH,
                  0,
                  null);
            });
  }

  private ImportRow row(int line, String topicName, String question, List<RubricCriterion> rubric) {
    return new ImportRow(
        line, topicName, "medium", question, rubric, true, List.of(), null, List.of(), List.of(),
        null, true);
  }

  private static ImportRow shaped(int line, String level, boolean rubricOk, boolean fieldsOk) {
    return new ImportRow(
        line,
        "Database",
        level,
        "Q" + line,
        List.of(),
        rubricOk,
        null,
        null,
        null,
        null,
        null,
        fieldsOk);
  }

  private static RubricCriterion criterion(String code, String weight) {
    return new RubricCriterion(code, new BigDecimal(weight), null);
  }

  private List<String> reasons(List<ImportRow> rows) {
    return service.run(new ImportCommand(actor, rows)).skippedRows().stream()
        .map(s -> s.line() + ":" + s.reasonCode())
        .toList();
  }

  @Test
  void everyRejectionReasonIsReportedWithItsLineAndGoodRowsStillImport() {
    var rows =
        List.of(
            row(2, "Database", "Fresh question", List.of(criterion("A", "100"))),
            row(3, "Unknown", "Q3", List.of()),
            row(4, "database", "Q4", List.of(criterion("A", "30"), criterion("B", "30"))),
            row(5, "DATABASE", "EXISTING", List.of()),
            row(6, "Database", " ", List.of()),
            row(7, "Database", "Q7", List.of(criterion("A", "50"), criterion("a", "50"))),
            row(8, "Database", "x".repeat(256), List.of()),
            shaped(9, "nolevel", true, true),
            shaped(10, "medium", false, true),
            shaped(11, "medium", true, false));
    var result = service.run(new ImportCommand(actor, rows));
    assertThat(result.importedCount()).isEqualTo(1);
    assertThat(result.skippedRows())
        .extracting(s -> s.line() + ":" + s.reasonCode())
        .containsExactly(
            "3:unknownTopic",
            "4:rubricSum",
            "5:duplicate",
            "6:missingField",
            "7:badRubric",
            "8:invalidField",
            "9:unknownLevel",
            "10:badRubric",
            "11:invalidField");
    verify(repository).lockImport();
  }

  @Test
  void sameTitleTwiceInOneFileImportsOnlyTheFirst() {
    var rows =
        List.of(
            row(2, "Database", "Same Title", List.of()),
            row(3, "Database", "same title", List.of()));
    assertThat(reasons(rows)).containsExactly("3:duplicate");
  }

  @Test
  void rubricEntriesAreChecked() {
    var bad =
        List.of(
            row(2, "Database", "A1", List.of(new RubricCriterion(null, BigDecimal.TEN, null))),
            row(3, "Database", "A2", List.of(new RubricCriterion("x", null, null))),
            row(4, "Database", "A3", List.of(criterion("x", "100.5"))),
            row(5, "Database", "A4", List.of(criterion("x", "-1"))),
            row(6, "Database", "A5", List.of(criterion("x", "12.345"))),
            row(7, "Database", "A6", List.of(criterion("y".repeat(101), "100"))));
    assertThat(reasons(bad)).allMatch(text -> text.endsWith(":badRubric")).hasSize(6);
  }

  @Test
  void storedValuesAreNormalisedAndApproachBecomesMarkdownList() {
    var rowWithLists =
        new ImportRow(
            2,
            "Database",
            "medium",
            "  Index? ",
            List.of(criterion(" Rõ ", "100")),
            true,
            List.of(" a ", ""),
            "",
            List.of("one", " two ", ""),
            List.of(" k "),
            " ",
            true);
    service.run(new ImportCommand(actor, List.of(rowWithLists)));
    var captor = ArgumentCaptor.forClass(QuestionDraft.class);
    verify(repository).insert(captor.capture(), any());
    var draft = captor.getValue();
    assertThat(draft.title()).isEqualTo("Index?");
    assertThat(draft.followUps()).containsExactly("a");
    assertThat(draft.contentMarkdown()).isNull();
    assertThat(draft.suggestedApproach()).isEqualTo("- one\n- two");
    assertThat(draft.coreKeywords()).containsExactly("k");
    assertThat(draft.sampleAnswerFramework()).isNull();
    assertThat(draft.rubric().get(0).criterionCode()).isEqualTo("Rõ");
  }

  @Test
  void emptyOptionalListsStoreNull() {
    service.run(new ImportCommand(actor, List.of(row(2, "Database", "Plain", List.of()))));
    var captor = ArgumentCaptor.forClass(QuestionDraft.class);
    verify(repository).insert(captor.capture(), any());
    assertThat(captor.getValue().suggestedApproach()).isNull();
    assertThat(captor.getValue().coreKeywords()).isNull();
  }

  @Test
  void oneAuditRowPerImportedQuestionAndNoneForSkipped() {
    var rows =
        List.of(
            row(2, "Database", "One", List.of()),
            row(3, "Database", "Two", List.of()),
            row(4, "Nope", "Three", List.of()));
    service.run(new ImportCommand(actor, rows));
    var captor = ArgumentCaptor.forClass(InterviewAuditEntry.class);
    verify(audit, org.mockito.Mockito.times(2)).record(captor.capture());
    assertThat(captor.getAllValues())
        .allSatisfy(
            entry -> {
              assertThat(entry.actionType()).isEqualTo("INTERVIEW_QUESTION_IMPORTED");
              assertThat(entry.actorUserId()).isEqualTo(actor);
            });
    assertThat(captor.getAllValues().get(1).afterJson()).contains("\"line\":3");
  }

  @Test
  void topicOrLevelDeletedMeanwhileSkipsTheRow() {
    when(repository.lockReference(CatalogKind.TOPIC, topic)).thenReturn(false);
    assertThat(reasons(List.of(row(2, "Database", "Q", List.of()))))
        .containsExactly("2:unknownTopic");
    when(repository.lockReference(CatalogKind.TOPIC, topic)).thenReturn(true);
    when(repository.lockReference(CatalogKind.LEVEL, level)).thenReturn(false);
    assertThat(reasons(List.of(row(2, "Database", "Q", List.of()))))
        .containsExactly("2:unknownLevel");
    verify(repository, never()).insert(any(), any());
  }

  @Test
  void handlerDelegates() {
    var result =
        new ImportQuestionsCommandHandler(service)
            .execute(
                new ImportCommand(actor, List.of(row(2, "Database", "Via handler", List.of()))));
    assertThat(result.importedCount()).isEqualTo(1);
  }
}
