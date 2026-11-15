package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StudentCatalogModels.Bookmark;
import com.algoprep.problembank.domain.model.StudentCatalogModels.BookmarkSaved;
import com.algoprep.problembank.domain.ports.out.BookmarkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Student bookmarks (DD F2 bookmark endpoints): filters, paging, note rules, idempotent state. */
class MyBookmarksHandlerTest {
  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

  private final BookmarkRepository repo = mock(BookmarkRepository.class);
  private final SolveStatePort states = mock(SolveStatePort.class);
  private final MyBookmarksHandler handler = new MyBookmarksHandler(repo, states);
  private final UUID user = UUID.randomUUID();

  private static UUID id(int n) {
    return new UUID(0, n);
  }

  private static Bookmark bookmark(int n, String code, String title, String level, String note) {
    return new Bookmark(
        id(n), code, title, level, level, List.of("Array"), note, T0.plusSeconds(n), n == 99);
  }

  private void saved(Bookmark... all) {
    when(repo.list(user)).thenReturn(List.of(all));
  }

  @Test
  void listWithoutSolveStatesHidesStateAndSolvedCountAndSortsNewestFirst() {
    when(states.mine(user)).thenReturn(Optional.empty());
    saved(bookmark(1, "A", "Alpha", "EASY", null), bookmark(2, "B", "Beta", "HARD", "n"));

    var list = handler.list(user, Map.of("status", "SOLVED"));

    assertThat(list.items()).extracting("code").containsExactly("B", "A");
    assertThat(list.items().get(0).solveState()).isNull();
    assertThat(list.items().get(0).note()).isEqualTo("n");
    assertThat(list.items().get(1).note()).isEmpty();
    assertThat(list.meta().solvedCount()).isNull();
    assertThat(list.meta().savedTotal()).isEqualTo(2);
  }

  @Test
  void ascendingOrderAndSearchMatchCodeOrTitleCaseInsensitively() {
    when(states.mine(user)).thenReturn(Optional.empty());
    saved(
        bookmark(1, "TWO-SUM", "Pair", "EASY", null),
        bookmark(2, "X", "two pointers", "EASY", null),
        bookmark(3, "Y", "Other", "EASY", null));

    var list = handler.list(user, Map.of("q", "  TWO ", "dir", "asc"));

    assertThat(list.items()).extracting("code").containsExactly("TWO-SUM", "X");
    assertThat(list.meta().filteredTotal()).isEqualTo(2);
  }

  @Test
  void difficultyFilterAppliesOnlyWhenTheLevelCodeExists() {
    when(states.mine(user)).thenReturn(Optional.empty());
    saved(bookmark(1, "A", "a", "EASY", null), bookmark(2, "B", "b", "HARD", null));
    when(repo.levelCodeExists("EASY")).thenReturn(true);
    when(repo.levelCodeExists("NOPE")).thenReturn(false);

    assertThat(handler.list(user, Map.of("difficulty", "EASY")).items())
        .extracting("code")
        .containsExactly("A");
    assertThat(handler.list(user, Map.of("difficulty", "NOPE")).items()).hasSize(2);
    assertThat(handler.list(user, Map.of("difficulty", "   ")).items()).hasSize(2);
  }

  @Test
  void statusFilterUsesSolvedAttemptedUnsolvedAndIgnoresUnknownValues() {
    var states0 = new SolveStates(Set.of(id(1)), Set.of(id(2)));
    when(states.mine(user)).thenReturn(Optional.of(states0));
    saved(
        bookmark(1, "A", "a", "EASY", null),
        bookmark(2, "B", "b", "EASY", null),
        bookmark(3, "C", "c", "EASY", null));

    assertThat(handler.list(user, Map.of("status", "SOLVED")).items())
        .extracting("code")
        .containsExactly("A");
    assertThat(handler.list(user, Map.of("status", "ATTEMPTED")).items())
        .extracting("code")
        .containsExactly("B");
    assertThat(handler.list(user, Map.of("status", "UNSOLVED")).items())
        .extracting("code")
        .containsExactly("C");
    var everything = handler.list(user, Map.of("status", "BOGUS"));
    assertThat(everything.items()).hasSize(3);
    assertThat(everything.meta().solvedCount()).isEqualTo(1);
    assertThat(everything.items())
        .extracting("solveState")
        .containsExactly("UNSOLVED", "ATTEMPTED", "SOLVED");
  }

  @Test
  void pagesHoldTwentyItemsAndOutOfRangeOrGarbagePagesAreClamped() {
    when(states.mine(user)).thenReturn(Optional.empty());
    var all = new ArrayList<Bookmark>();
    for (int i = 1; i <= 45; i++) {
      all.add(bookmark(i, "C" + i, "t", "EASY", null));
    }
    when(repo.list(user)).thenReturn(all);

    var third = handler.list(user, Map.of("page", "3"));
    assertThat(third.items()).hasSize(5);
    assertThat(third.page().totalPages()).isEqualTo(3);
    assertThat(handler.list(user, Map.of("page", "99")).page().number()).isEqualTo(3);
    assertThat(handler.list(user, Map.of("page", "0")).page().number()).isEqualTo(1);
    assertThat(handler.list(user, Map.of("page", "abc")).page().number()).isEqualTo(1);
    assertThat(handler.list(user, Map.of()).items()).hasSize(20);
  }

  @Test
  void emptyBookmarkListStillHasOnePage() {
    when(states.mine(user)).thenReturn(Optional.empty());
    saved();

    var list = handler.list(user, Map.of());

    assertThat(list.items()).isEmpty();
    assertThat(list.page().totalPages()).isEqualTo(1);
    assertThat(list.meta().savedTotal()).isZero();
  }

  @Test
  void overlongSearchTextIsTruncatedToOneHundredCharacters() {
    when(states.mine(user)).thenReturn(Optional.empty());
    saved(bookmark(1, "A", "x".repeat(100), "EASY", null));

    assertThat(handler.list(user, Map.of("q", "x".repeat(300))).items()).hasSize(1);
  }

  @Test
  void withdrawnBookmarksStayInTheListWithTheFlag() {
    when(states.mine(user)).thenReturn(Optional.empty());
    saved(bookmark(99, "W", "withdrawn", "EASY", null));

    assertThat(handler.list(user, Map.of()).items().get(0).withdrawn()).isTrue();
  }

  @Test
  void saveUpdatesExistingNoteTrimmedAndReportsNotCreated() {
    var problem = id(5);
    when(repo.updateNote(user, problem, "hello"))
        .thenReturn(Optional.of(new BookmarkSaved("hello", T0, false)));

    var result = handler.save(user, problem, "  hello  ", false);

    assertThat(result.created()).isFalse();
    assertThat(result.bookmark().note()).isEqualTo("hello");
    verify(repo, never()).insert(any(), any(), any());
  }

  @Test
  void blankNoteIsStoredAsNull() {
    var problem = id(5);
    when(repo.updateNote(user, problem, null))
        .thenReturn(Optional.of(new BookmarkSaved(null, T0, false)));

    assertThat(handler.save(user, problem, "   ", false).bookmark().note()).isNull();
    handler.save(user, problem, null, false);
    verify(repo, org.mockito.Mockito.times(2)).updateNote(user, problem, null);
  }

  @Test
  void noteLongerThanOneThousandCharactersIsRejectedButExactlyOneThousandIsAccepted() {
    var problem = id(5);
    when(repo.updateNote(any(), any(), any()))
        .thenReturn(Optional.of(new BookmarkSaved("x", T0, false)));

    assertThat(handler.save(user, problem, "n".repeat(1000), false)).isNotNull();
    assertThatThrownBy(() -> handler.save(user, problem, "n".repeat(1001), false))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void saveOfMissingBookmarkIsNotFoundWhenCreationIsNotRequested() {
    assertThatThrownBy(() -> handler.save(user, id(5), "n", false))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Bookmark");
  }

  @Test
  void saveCreatesForPublishedProblemAndReportsCreated() {
    var problem = id(5);
    when(repo.problemPublished(problem)).thenReturn(true);
    when(repo.insert(user, problem, "n")).thenReturn(new BookmarkSaved("n", T0, true));

    var result = handler.save(user, problem, "n", true);

    assertThat(result.created()).isTrue();
    assertThat(result.bookmark().problemId()).isEqualTo(problem);
  }

  @Test
  void saveCannotBookmarkAnUnpublishedProblem() {
    assertThatThrownBy(() -> handler.save(user, id(5), "n", true))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(repo, never()).insert(any(), any(), any());
  }

  @Test
  void removeDeletesWithoutChecking() {
    handler.remove(user, id(5));

    verify(repo).delete(user, id(5));
  }

  @Test
  void setStateFalseDeletesAndTrueInsertsOnlyWhenMissing() {
    var problem = id(5);
    assertThat(handler.setState(user, problem, false).bookmarked()).isFalse();
    verify(repo).delete(user, problem);

    when(repo.exists(user, problem)).thenReturn(true);
    assertThat(handler.setState(user, problem, true).bookmarked()).isTrue();
    verify(repo, never()).insert(any(), any(), any());

    when(repo.exists(user, problem)).thenReturn(false);
    when(repo.problemPublished(problem)).thenReturn(true);
    assertThat(handler.setState(user, problem, true).bookmarked()).isTrue();
    verify(repo).insert(user, problem, null);
  }

  @Test
  void setStateTrueOnUnpublishedProblemIsNotFound() {
    assertThatThrownBy(() -> handler.setState(user, id(5), true))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
