package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.StudentCatalogModels.Bookmark;
import com.algoprep.problembank.domain.model.StudentCatalogModels.BookmarkSaved;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bookmarks of one user; the user id always comes from the token, never from a request. */
public interface BookmarkRepository {
  /** Every bookmark of the user, withdrawn problems included, newest first. */
  List<Bookmark> list(UUID userId);

  boolean levelCodeExists(String code);

  /** Changes the note of an existing bookmark, keeping its saved time; empty when there is none. */
  Optional<BookmarkSaved> updateNote(UUID userId, UUID problemId, String note);

  /** Creates the bookmark, or updates the note if a concurrent call created it first. */
  BookmarkSaved insert(UUID userId, UUID problemId, String note);

  boolean problemPublished(UUID problemId);

  boolean exists(UUID userId, UUID problemId);

  void delete(UUID userId, UUID problemId);
}
