package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.StudentCatalogViews.BookmarkItem;
import com.algoprep.problembank.application.dto.StudentCatalogViews.BookmarkList;
import com.algoprep.problembank.application.dto.StudentCatalogViews.BookmarkMeta;
import com.algoprep.problembank.application.dto.StudentCatalogViews.BookmarkSaveResult;
import com.algoprep.problembank.application.dto.StudentCatalogViews.BookmarkSavedView;
import com.algoprep.problembank.application.dto.StudentCatalogViews.BookmarkState;
import com.algoprep.problembank.application.dto.StudentProblemViews.PageMeta;
import com.algoprep.problembank.application.ports.in.MyBookmarksUseCase;
import com.algoprep.problembank.application.ports.out.SolveStatePort;
import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StudentCatalogModels.Bookmark;
import com.algoprep.problembank.domain.ports.out.BookmarkRepository;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 sections 9 to 12. The list reads the caller's rows (bounded by what one learner saved) and
 * filters in memory, because the solve state lives in another module. Page size is fixed at 20.
 */
@Service
class MyBookmarksHandler implements MyBookmarksUseCase {
  static final int PAGE_SIZE = 20;
  static final int NOTE_MAX = 1000;
  private static final Set<String> STATUSES = Set.of("SOLVED", "ATTEMPTED", "UNSOLVED");

  private final BookmarkRepository bookmarks;
  private final SolveStatePort solveStates;

  MyBookmarksHandler(BookmarkRepository bookmarks, SolveStatePort solveStates) {
    this.bookmarks = bookmarks;
    this.solveStates = solveStates;
  }

  @Override
  @Transactional(readOnly = true)
  public BookmarkList list(UUID userId, Map<String, String> params) {
    var states = solveStates.mine(userId).orElse(null);
    var all = bookmarks.list(userId);
    String status = params.get("status");
    String q = text(params.get("q"));
    String difficulty = text(params.get("difficulty"));
    boolean level = difficulty != null && bookmarks.levelCodeExists(difficulty);
    var kept =
        all.stream()
            .filter(b -> q == null || matches(b, q))
            .filter(b -> !level || difficulty.equals(b.levelCode()))
            .filter(
                b ->
                    states == null
                        || status == null
                        || !STATUSES.contains(status)
                        || status.equals(stateOf(states, b.problemId())))
            .sorted(order(params.get("dir")))
            .toList();
    int totalPages = Math.max(1, (int) Math.ceil(kept.size() / (double) PAGE_SIZE));
    int page = Math.min(Math.max(integer(params.get("page")), 1), totalPages);
    var items =
        kept.stream()
            .skip((long) (page - 1) * PAGE_SIZE)
            .limit(PAGE_SIZE)
            .map(b -> item(b, states))
            .toList();
    Integer solvedCount =
        states == null
            ? null
            : (int) all.stream().filter(b -> states.solved().contains(b.problemId())).count();
    return new BookmarkList(
        items,
        new PageMeta(page, PAGE_SIZE, kept.size(), totalPages),
        new BookmarkMeta(all.size(), solvedCount, kept.size()));
  }

  @Override
  @Transactional
  public BookmarkSaveResult save(
      UUID userId, UUID problemId, String note, boolean createIfMissing) {
    String clean = note == null || note.isBlank() ? null : note.trim();
    if (clean != null && clean.length() > NOTE_MAX) {
      throw new InvalidRequestException("PRB-001", "Note is longer than 1000 characters");
    }
    var updated = bookmarks.updateNote(userId, problemId, clean);
    if (updated.isPresent()) {
      return result(problemId, updated.get().note(), updated.get().savedAt(), false);
    }
    if (!createIfMissing) {
      throw new ResourceNotFoundException("PRB-014", "Bookmark not found");
    }
    requirePublished(problemId);
    var created = bookmarks.insert(userId, problemId, clean);
    return result(problemId, created.note(), created.savedAt(), created.created());
  }

  @Override
  @Transactional
  public void remove(UUID userId, UUID problemId) {
    bookmarks.delete(userId, problemId);
  }

  @Override
  @Transactional
  public BookmarkState setState(UUID userId, UUID problemId, boolean bookmarked) {
    if (!bookmarked) {
      bookmarks.delete(userId, problemId);
      return new BookmarkState(false);
    }
    if (!bookmarks.exists(userId, problemId)) {
      requirePublished(problemId);
      bookmarks.insert(userId, problemId, null);
    }
    return new BookmarkState(true);
  }

  private void requirePublished(UUID problemId) {
    if (!bookmarks.problemPublished(problemId)) {
      throw new ResourceNotFoundException("PRB-010", "Problem not found");
    }
  }

  private static BookmarkSaveResult result(
      UUID problemId, String note, java.time.Instant savedAt, boolean created) {
    return new BookmarkSaveResult(new BookmarkSavedView(problemId, note, savedAt), created);
  }

  private static BookmarkItem item(Bookmark b, SolveStates states) {
    return new BookmarkItem(
        b.problemId(),
        b.code(),
        b.title(),
        b.levelCode(),
        b.levelDisplayName(),
        b.topicNames(),
        states == null ? null : stateOf(states, b.problemId()),
        b.note() == null ? "" : b.note(),
        b.savedAt(),
        b.withdrawn());
  }

  private static Comparator<Bookmark> order(String dir) {
    Comparator<Bookmark> byTime =
        Comparator.comparing(Bookmark::savedAt).thenComparing(Bookmark::problemId);
    return "asc".equals(dir) ? byTime : byTime.reversed();
  }

  private static boolean matches(Bookmark b, String q) {
    String needle = q.toLowerCase(Locale.ROOT);
    return b.code().toLowerCase(Locale.ROOT).contains(needle)
        || b.title().toLowerCase(Locale.ROOT).contains(needle);
  }

  private static String stateOf(SolveStates states, UUID id) {
    if (states.solved().contains(id)) {
      return "SOLVED";
    }
    return states.attempted().contains(id) ? "ATTEMPTED" : "UNSOLVED";
  }

  private static String text(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.trim();
    return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
  }

  private static int integer(String raw) {
    try {
      return raw == null ? 1 : Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      return 1;
    }
  }
}
