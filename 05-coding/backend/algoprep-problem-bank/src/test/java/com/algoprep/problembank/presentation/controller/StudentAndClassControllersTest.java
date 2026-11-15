package com.algoprep.problembank.presentation.controller;

import static com.algoprep.problembank.presentation.controller.ControllerSupport.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.assignment.ClassAssignmentViews;
import com.algoprep.problembank.application.dto.student.StudentCatalogViews;
import com.algoprep.problembank.application.ports.in.assignment.ManageClassAssignmentsUseCase;
import com.algoprep.problembank.application.ports.in.assignment.MyClassAssignmentsUseCase;
import com.algoprep.problembank.application.ports.in.assignment.ReadClassAssignmentsUseCase;
import com.algoprep.problembank.application.ports.in.student.MyBookmarksUseCase;
import com.algoprep.problembank.application.ports.in.student.ReadPublishedProblemsUseCase;
import com.algoprep.problembank.application.ports.in.student.StudentCatalogUseCase;
import com.algoprep.problembank.application.ports.in.student.SuggestedProblemsUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Student catalogue, bookmarks, published problems, suggestions and class assignment endpoints. */
class StudentAndClassControllersTest {
  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID P = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID CLASS = UUID.fromString("00000000-0000-0000-0000-000000000002");

  // ---- published problems ----

  private final ReadPublishedProblemsUseCase published = mock(ReadPublishedProblemsUseCase.class);
  private final PublishedProblemController problems = new PublishedProblemController(published);

  @Test
  void publishedProblemEndpointsForwardTheCallerAndParameters() {
    var params = Map.of("q", "two");
    var caller = jwt(USER);

    problems.list(caller, params);
    problems.random(caller, params);
    problems.workspace(caller, P.toString());
    problems.overview(caller, P.toString());
    problems.samples(P.toString());

    verify(published).list(USER, params);
    verify(published).random(USER, params);
    verify(published).workspace(USER, P);
    verify(published).overview(USER, P);
    verify(published).samples(P);
  }

  @Test
  void publishedProblemWithMalformedIdIsNotFoundPrb010() {
    var caller = jwt(USER);

    assertThatThrownBy(() -> problems.workspace(caller, "x"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> problems.overview(caller, "x"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> problems.samples("x")).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void suggestionsAreForTheTokenSubject() {
    var suggestions = mock(SuggestedProblemsUseCase.class);

    new SuggestedProblemController(suggestions).suggested(jwt(USER), Map.of("topic", "t"));

    verify(suggestions).suggest(USER, Map.of("topic", "t"));
  }

  // ---- student catalogue and bookmarks ----

  private final StudentCatalogUseCase catalog = mock(StudentCatalogUseCase.class);
  private final MyBookmarksUseCase bookmarks = mock(MyBookmarksUseCase.class);
  private final StudentCatalogController student = new StudentCatalogController(catalog, bookmarks);

  @Test
  void catalogProgressAndSummaryDelegate() {
    student.topicProgress(jwt(USER));
    student.levelProgress(jwt(USER));
    student.summary();
    student.list(jwt(USER), Map.of("page", "2"));

    verify(catalog).topicProgress(USER);
    verify(catalog).levelProgress(USER);
    verify(catalog).summary();
    verify(bookmarks).list(USER, Map.of("page", "2"));
  }

  @Test
  void saveBookmarkIs201WhenCreatedAnd200WhenUpdated() {
    var view = new StudentCatalogViews.BookmarkSavedView(P, "n", Instant.EPOCH);
    when(bookmarks.save(USER, P, "n", true))
        .thenReturn(new StudentCatalogViews.BookmarkSaveResult(view, true));
    when(bookmarks.save(USER, P, "n", false))
        .thenReturn(new StudentCatalogViews.BookmarkSaveResult(view, false));

    var created =
        student.save(jwt(USER), P.toString(), new StudentCatalogController.NoteRequest("n", null));
    var updated =
        student.save(jwt(USER), P.toString(), new StudentCatalogController.NoteRequest("n", false));

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody()).isEqualTo(view);
    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void saveBookmarkWithoutBodyCreatesWithNullNote() {
    var view = new StudentCatalogViews.BookmarkSavedView(P, null, Instant.EPOCH);
    when(bookmarks.save(USER, P, null, true))
        .thenReturn(new StudentCatalogViews.BookmarkSaveResult(view, true));

    assertThat(student.save(jwt(USER), P.toString(), null).getStatusCode())
        .isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void removeBookmarkIs204AndMalformedIdIs404() {
    var response = student.remove(jwt(USER), P.toString());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(bookmarks).remove(USER, P);
    assertThatThrownBy(() -> student.remove(jwt(USER), "q"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> student.save(jwt(USER), "q", null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void bookmarkStateRequiresTheBooleanFlag() {
    student.state(jwt(USER), P.toString(), new StudentCatalogController.StateRequest(true));
    verify(bookmarks).setState(USER, P, true);

    assertThatThrownBy(
            () ->
                student.state(
                    jwt(USER), P.toString(), new StudentCatalogController.StateRequest(null)))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> student.state(jwt(USER), P.toString(), null))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
            () -> student.state(jwt(USER), "q", new StudentCatalogController.StateRequest(false)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  // ---- my class assignments ----

  @Test
  void myClassAssignmentsDelegateWithRawQueryStrings() {
    var mine = mock(MyClassAssignmentsUseCase.class);
    var controller = new MyClassAssignmentController(mine);

    controller.groups(jwt(USER));
    controller.problems(jwt(USER), CLASS.toString(), "2", "10");
    controller.problems(jwt(USER), null, null, null);

    verify(mine).groups(USER);
    verify(mine).problems(USER, CLASS.toString(), "2", "10");
    verify(mine).problems(USER, null, null, null);
  }

  // ---- instructor class assignments ----

  private final ReadClassAssignmentsUseCase reads = mock(ReadClassAssignmentsUseCase.class);
  private final ManageClassAssignmentsUseCase writes = mock(ManageClassAssignmentsUseCase.class);
  private final ClassAssignmentController assignments =
      new ClassAssignmentController(reads, writes);

  @Test
  void instructorReadsForwardTheCaller() {
    var params = Map.of("q", "x");

    assignments.list(jwt(USER), params);
    assignments.summary(jwt(USER));
    assignments.top(jwt(USER));
    assignments.assignable(jwt(USER), CLASS.toString(), params);

    verify(reads).list(USER, params);
    verify(reads).summary(USER);
    verify(reads).top(USER);
    verify(reads).assignable(USER, CLASS, params);
  }

  @Test
  void aMalformedClassIdIsTreatedAsAClassTheCallerDoesNotOwnPrb005() {
    var caller = jwt(USER);

    assertThatThrownBy(() -> assignments.assignable(caller, "x", Map.of()))
        .isInstanceOf(PermissionDeniedException.class)
        .hasMessageContaining("not responsible");
    assertThatThrownBy(
            () ->
                assignments.assign(
                    caller, "x", new ClassAssignmentController.AssignRequest(List.of())))
        .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> assignments.remove(caller, "x", P.toString()))
        .isInstanceOf(PermissionDeniedException.class);
  }

  @Test
  void assignForwardsProblemIdsAndRemoveRespondsNoContent() {
    var result = mock(ClassAssignmentViews.AssignResult.class);
    when(writes.assign(USER, CLASS, List.of(P))).thenReturn(result);

    assertThat(
            assignments.assign(
                jwt(USER),
                CLASS.toString(),
                new ClassAssignmentController.AssignRequest(List.of(P))))
        .isSameAs(result);
    var response = assignments.remove(jwt(USER), CLASS.toString(), P.toString());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(writes).remove(USER, CLASS, P);
  }

  @Test
  void removeOfAMalformedProblemIdUsesTheNilUuidSoTheUseCaseReportsNotAssigned() {
    assignments.remove(jwt(USER), CLASS.toString(), "not-a-uuid");

    verify(writes).remove(USER, CLASS, new UUID(0, 0));
  }
}
