package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.StudentCatalogViews;
import com.algoprep.problembank.application.ports.in.MyBookmarksUseCase;
import com.algoprep.problembank.application.ports.in.StudentCatalogUseCase;
import com.algoprep.problembank.domain.model.StudentCatalogModels.CatalogSummary;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Learner reads and bookmarks, #3, #8 to #11, #13 and the level progress (DD F2 sections 4, 9 to
 * 12, 14, 14A). The user is always the token subject; no route takes a user id.
 */
@RestController
@RequestMapping("/api/v1/problem-bank")
@PreAuthorize("isAuthenticated()")
class StudentCatalogController {
  private final StudentCatalogUseCase catalog;
  private final MyBookmarksUseCase bookmarks;

  StudentCatalogController(StudentCatalogUseCase catalog, MyBookmarksUseCase bookmarks) {
    this.catalog = catalog;
    this.bookmarks = bookmarks;
  }

  record NoteRequest(String note, Boolean createIfMissing) {}

  record StateRequest(Boolean bookmarked) {}

  @GetMapping("/topics/progress")
  StudentCatalogViews.TopicProgressList topicProgress(JwtAuthenticationToken caller) {
    return catalog.topicProgress(user(caller));
  }

  @GetMapping("/me/levels/progress")
  StudentCatalogViews.MyLevelProgress levelProgress(JwtAuthenticationToken caller) {
    return catalog.levelProgress(user(caller));
  }

  @GetMapping("/catalog/summary")
  CatalogSummary summary() {
    return catalog.summary();
  }

  @GetMapping("/me/bookmarks")
  StudentCatalogViews.BookmarkList list(
      JwtAuthenticationToken caller, @RequestParam Map<String, String> params) {
    return bookmarks.list(user(caller), params);
  }

  @PutMapping("/me/bookmarks/{problemId}")
  ResponseEntity<StudentCatalogViews.BookmarkSavedView> save(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody(required = false) NoteRequest body) {
    var request = body == null ? new NoteRequest(null, null) : body;
    var saved =
        bookmarks.save(
            user(caller),
            problem(problemId),
            request.note(),
            !Boolean.FALSE.equals(request.createIfMissing()));
    return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(saved.bookmark());
  }

  @DeleteMapping("/me/bookmarks/{problemId}")
  ResponseEntity<Void> remove(JwtAuthenticationToken caller, @PathVariable String problemId) {
    bookmarks.remove(user(caller), problem(problemId));
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/me/bookmarks/{problemId}/state")
  StudentCatalogViews.BookmarkState state(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody StateRequest body) {
    if (body == null || body.bookmarked() == null) {
      throw new com.algoprep.common.exception.InvalidRequestException(
          "PRB-001", "bookmarked is required");
    }
    return bookmarks.setState(user(caller), problem(problemId), body.bookmarked());
  }

  private static UUID user(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  private static UUID problem(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException("PRB-010", "Problem not found");
    }
  }
}
