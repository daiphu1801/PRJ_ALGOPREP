package com.algoprep.interviewbank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.interviewbank.application.dto.AdminQuestionQuery;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.model.AdminQuestionPage;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.QuestionListCriteria;
import com.algoprep.interviewbank.domain.model.QuestionSearch;
import com.algoprep.interviewbank.domain.model.QuestionSort;
import com.algoprep.interviewbank.domain.ports.out.AdminQuestionRepository;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AdminQuestionQueryHandlersTest {
  private final AdminQuestionRepository repository = mock(AdminQuestionRepository.class);
  private final CatalogRepository catalog = mock(CatalogRepository.class);
  private final ListAdminQuestionsQueryHandler list =
      new ListAdminQuestionsQueryHandler(repository, catalog);
  private final UUID topic = UUID.randomUUID();

  private AdminQuestionQuery query(String q, String topicCode, String level) {
    return new AdminQuestionQuery(q, topicCode, level, QuestionSort.LEVEL, true, 2, 20);
  }

  @Test
  void searchTextBecomesCleanPrefixTokensOnly() {
    assertThat(QuestionSearch.tsQuery("  Câu hỏi: index! ")).isEqualTo("câu:* & hỏi:* & index:*");
    assertThat(QuestionSearch.tsQuery("a' | b & !c")).isEqualTo("a:* & b:* & c:*");
    assertThat(QuestionSearch.tsQuery("!!! %")).isNull();
    assertThat(QuestionSearch.tsQuery(null)).isNull();
  }

  @Test
  void codesResolveToIdsAndPassThroughToTheRepository() {
    when(catalog.list(CatalogKind.TOPIC, false))
        .thenReturn(List.of(new CatalogItem(topic, "DATABASE", "Database", 1, false, null)));
    var page = new AdminQuestionPage(List.of(), 1, 20, 0, 0);
    when(repository.page(any())).thenReturn(page);
    assertThat(list.list(query("index", "DATABASE", null))).isSameAs(page);
    var captor = ArgumentCaptor.forClass(QuestionListCriteria.class);
    verify(repository).page(captor.capture());
    assertThat(captor.getValue().topicId()).isEqualTo(topic);
    assertThat(captor.getValue().levelId()).isNull();
    assertThat(captor.getValue().searchQuery()).isEqualTo("index:*");
    assertThat(captor.getValue().descending()).isTrue();
    assertThat(captor.getValue().page()).isEqualTo(2);
  }

  @Test
  void unknownCatalogueCodeIsInb001() {
    when(catalog.list(CatalogKind.LEVEL, false)).thenReturn(List.of());
    assertThatThrownBy(() -> list.list(query(null, null, "NOPE")))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-V01"));
  }

  @Test
  void detailOfRetiredOrMissingQuestionIsInb010() {
    var id = UUID.randomUUID();
    var handler = new GetAdminQuestionQueryHandler(repository);
    when(repository.findActive(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.get(id))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-010"));
    var found =
        new AdminQuestion(
            id,
            1,
            topic,
            topic,
            "T",
            null,
            List.of(),
            List.of(),
            Instant.EPOCH,
            Instant.EPOCH,
            0,
            null);
    when(repository.findActive(id)).thenReturn(Optional.of(found));
    assertThat(handler.get(id)).isSameAs(found);
  }
}
