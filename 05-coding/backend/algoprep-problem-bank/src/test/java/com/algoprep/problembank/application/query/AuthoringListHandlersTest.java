package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.ListProblemsQuery;
import com.algoprep.problembank.domain.model.AuthoringListFilter;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringListRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;

class AuthoringListHandlersTest {
  private final ProblemAuthoringListRepository repository =
      mock(ProblemAuthoringListRepository.class);
  private final ListAuthoringProblemsQueryHandler list =
      new ListAuthoringProblemsQueryHandler(repository);
  private final UUID actor = UUID.randomUUID();

  private ListProblemsQuery query(String q, String status, String sort, String dir) {
    return new ListProblemsQuery(actor, false, q, null, status, sort, dir, null, null);
  }

  private ListProblemsQuery paged(Integer page, Integer size) {
    return new ListProblemsQuery(actor, false, null, null, null, null, null, page, size);
  }

  private AuthoringListFilter sent() {
    var captor = ArgumentCaptor.forClass(AuthoringListFilter.class);
    verify(repository).list(captor.capture());
    return captor.getValue();
  }

  @Test
  void defaultsAreUpdatedAtDescendingPageOneOfEight() {
    list.list(query(null, null, null, null));

    var f = sent();
    assertThat(f.sort()).isEqualTo("updatedAt");
    assertThat(f.ascending()).isFalse();
    assertThat(f.page()).isEqualTo(1);
    assertThat(f.pageSize()).isEqualTo(8);
    assertThat(f.q()).isNull();
  }

  @Test
  void anInstructorIsScopedToOwnProblemsAndAnAdminIsNot() {
    list.list(query(null, null, null, null));
    assertThat(sent().authorScope()).isEqualTo(actor);

    var admin = new ListProblemsQuery(actor, true, null, null, null, null, null, null, null);
    var other = mock(ProblemAuthoringListRepository.class);
    new ListAuthoringProblemsQueryHandler(other).list(admin);
    var captor = ArgumentCaptor.forClass(AuthoringListFilter.class);
    verify(other).list(captor.capture());
    assertThat(captor.getValue().authorScope()).isNull();
  }

  @Test
  void aSearchOfExactlyHundredCharactersPassesAndHundredAndOneIsRejected() {
    list.list(query("x".repeat(100), null, null, null));
    assertThat(sent().q()).hasSize(100);

    assertThatThrownBy(() -> list.list(query("x".repeat(101), null, null, null)))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-001"));
  }

  @Test
  void aBlankSearchMeansNoSearch() {
    list.list(query("   ", null, null, null));

    assertThat(sent().q()).isNull();
  }

  @Test
  void everyBadParameterIsRejectedStrictlyBeforeAnyRead() {
    var bad =
        new ListProblemsQuery[] {
          query(null, "DRAFT", null, null),
          query(null, null, "title; DROP TABLE", null),
          query(null, null, null, "sideways"),
          paged(0, null),
          paged(null, 7),
          new ListProblemsQuery(actor, false, null, "not-a-uuid", null, null, null, null, null),
        };

    for (var q : bad) {
      assertThatThrownBy(() -> list.list(q))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-001"));
    }
    verifyNoInteractions(repository);
  }

  @Test
  void allSevenSortKeysAndTheThreePageSizesAreAccepted() {
    for (String key :
        new String[] {
          "code", "title", "level", "status", "submissionCount", "acRate", "updatedAt"
        }) {
      list.list(query(null, null, key, "asc"));
    }
    for (int size : new int[] {8, 20, 50}) {
      list.list(paged(2, size));
    }
    org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(10)).list(any());
  }

  @Test
  void attentionUsesDefaultThresholdsAndTheScope() {
    var handler = new GetAttentionListQueryHandler(repository, new MockEnvironment());

    handler.get(new AuthoringScope(actor, false));

    verify(repository).attention(actor, 30, 7);
  }

  @Test
  void attentionThresholdsAreConfigurableAndAnAdminReadsTheWholeBank() {
    var env =
        new MockEnvironment()
            .withProperty("algoprep.problem-bank.attention.low-ac-percent", "45")
            .withProperty("algoprep.problem-bank.attention.stale-draft-days", "14");

    new GetAttentionListQueryHandler(repository, env).get(new AuthoringScope(actor, true));

    verify(repository).attention(null, 45, 14);
  }

  @Test
  void distributionPassesTheAuthorScope() {
    new GetTopicDistributionQueryHandler(repository).get(new AuthoringScope(actor, false));
    verify(repository).topicDistribution(actor);

    new GetTopicDistributionQueryHandler(repository).get(new AuthoringScope(actor, true));
    verify(repository).topicDistribution(null);
  }
}
