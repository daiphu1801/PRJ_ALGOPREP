package com.algoprep.interviewbank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.dto.LearnerQuestionQuery;
import com.algoprep.interviewbank.domain.model.CatalogItem;
import com.algoprep.interviewbank.domain.model.CatalogKind;
import com.algoprep.interviewbank.domain.model.LearnerQuestionCriteria;
import com.algoprep.interviewbank.domain.model.LearnerQuestionPage;
import com.algoprep.interviewbank.domain.model.QuestionSearch;
import com.algoprep.interviewbank.domain.model.RecallFilter;
import com.algoprep.interviewbank.domain.ports.out.CatalogRepository;
import com.algoprep.interviewbank.domain.ports.out.LearnerQuestionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InterviewQuestionQueryHandlersTest {
  private final LearnerQuestionRepository repository = mock(LearnerQuestionRepository.class);
  private final CatalogRepository catalog = mock(CatalogRepository.class);
  private final ListInterviewQuestionsQueryHandler list =
      new ListInterviewQuestionsQueryHandler(repository, catalog);
  private final UUID user = UUID.randomUUID();
  private final UUID topic = UUID.randomUUID();

  private LearnerQuestionQuery query(String q, String topicCode, boolean drill, int limit) {
    return new LearnerQuestionQuery(
        user, q, RecallFilter.REVIEWING, topicCode, "NOPE", true, 0, drill, limit);
  }

  private LearnerQuestionCriteria run(LearnerQuestionQuery query) {
    when(repository.page(any())).thenReturn(new LearnerQuestionPage(List.of(), 1, 20, 0, 0));
    list.list(query);
    var captor = ArgumentCaptor.forClass(LearnerQuestionCriteria.class);
    verify(repository, org.mockito.Mockito.atLeastOnce()).page(captor.capture());
    return captor.getValue();
  }

  @Test
  void unknownCodesAreIgnoredAndKnownOnesResolve() {
    when(catalog.list(CatalogKind.TOPIC, false))
        .thenReturn(List.of(new CatalogItem(topic, "DATABASE", "Database", 1, false, null)));
    when(catalog.list(CatalogKind.LEVEL, false)).thenReturn(List.of());
    var criteria = run(query("index", "DATABASE", false, 10));
    assertThat(criteria.topicId()).isEqualTo(topic);
    assertThat(criteria.levelId()).isNull();
    assertThat(criteria.userId()).isEqualTo(user);
    assertThat(criteria.status()).isEqualTo(RecallFilter.REVIEWING);
    assertThat(criteria.bookmarkedOnly()).isTrue();
    assertThat(criteria.page()).isEqualTo(1);
    assertThat(criteria.pageSize()).isEqualTo(20);
    assertThat(criteria.tsQuery()).isEqualTo("index:*");
  }

  @Test
  void searchTextIsCutToOneHundredCharactersAndLikeEscaped() {
    var criteria = run(query("a_%".repeat(50), null, false, 10));
    assertThat(criteria.likeText()).hasSize(166).startsWith("a\\_\\%a");
    assertThat(QuestionSearch.escapeLike("a_b%c\\d")).isEqualTo("a\\_b\\%c\\\\d");
  }

  @Test
  void textWithoutTokensDropsTheWholeSearch() {
    var criteria = run(query("!!! %_", null, false, 10));
    assertThat(criteria.tsQuery()).isNull();
    assertThat(criteria.likeText()).isNull();
  }

  @Test
  void drillLimitIsClampedToOneThroughTwenty() {
    assertThat(run(query(null, null, true, 99)).pageSize()).isEqualTo(20);
    assertThat(run(query(null, null, true, -5)).pageSize()).isEqualTo(1);
    assertThat(run(query(null, null, true, 7)).pageSize()).isEqualTo(7);
    assertThat(run(query(null, null, true, 7)).drill()).isTrue();
  }

  @Test
  void detailOfAMissingOrRetiredQuestionIsInb010() {
    var handler = new GetInterviewQuestionQueryHandler(repository);
    UUID id = UUID.randomUUID();
    when(repository.find(user, id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.get(user, id))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-010"));
  }
}
