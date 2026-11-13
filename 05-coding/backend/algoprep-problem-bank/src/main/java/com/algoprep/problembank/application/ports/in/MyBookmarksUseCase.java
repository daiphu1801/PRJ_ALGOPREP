package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.StudentCatalogViews;
import java.util.Map;
import java.util.UUID;

/** The caller's own bookmarks (DD F2 sections 9 to 12). */
public interface MyBookmarksUseCase {
  /** Parameters are read leniently, like the problem list. */
  StudentCatalogViews.BookmarkList list(UUID userId, Map<String, String> params);

  /**
   * {@code note} null or blank clears it; {@code createIfMissing} false never creates (404
   * PRB-014).
   */
  StudentCatalogViews.BookmarkSaveResult save(
      UUID userId, UUID problemId, String note, boolean createIfMissing);

  /** Idempotent: a bookmark that is already gone is not an error. */
  void remove(UUID userId, UUID problemId);

  /** Sets the wanted state rather than flipping it, so a repeat or a second device is harmless. */
  StudentCatalogViews.BookmarkState setState(UUID userId, UUID problemId, boolean bookmarked);
}
