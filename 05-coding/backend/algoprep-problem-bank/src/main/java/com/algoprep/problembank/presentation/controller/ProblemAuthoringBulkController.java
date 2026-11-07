package com.algoprep.problembank.presentation.controller;

import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.BulkProblemActionResult;
import com.algoprep.problembank.application.dto.ExportProblemsQuery;
import com.algoprep.problembank.application.ports.in.BulkProblemActionsUseCase;
import com.algoprep.problembank.application.ports.in.ExportProblemsCsvUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Export #27 and the five bulk actions #28 to #32 (DD F2 sections 28 to 33). A refused problem
 * never fails the call: the answer is 200 with {@code failedItems}.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/authoring/problems")
class ProblemAuthoringBulkController {
  private final BulkProblemActionsUseCase bulk;
  private final ExportProblemsCsvUseCase export;

  ProblemAuthoringBulkController(BulkProblemActionsUseCase bulk, ExportProblemsCsvUseCase export) {
    this.bulk = bulk;
    this.export = export;
  }

  record FilterRequest(String q, String level, String status) {}

  /** {@code problemIds} or {@code filter}, exactly one (export). */
  record ExportRequest(List<UUID> problemIds, FilterRequest filter) {}

  /** Body of every bulk action; {@code levelId} and {@code topicId} belong to their own action. */
  record BulkRequest(List<UUID> problemIds, UUID levelId, UUID topicId) {}

  @PostMapping("/export")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:READ')")
  ResponseEntity<byte[]> export(JwtAuthenticationToken caller, @RequestBody ExportRequest body) {
    var f =
        body.filter() == null
            ? null
            : new ExportProblemsQuery.Filter(
                body.filter().q(), body.filter().level(), body.filter().status());
    var csv = export.export(new ExportProblemsQuery(scope(caller), body.problemIds(), f));
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(csv.filename()).build().toString())
        .body(csv.content());
  }

  @PostMapping("/bulk/publish")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  BulkProblemActionResult publish(JwtAuthenticationToken caller, @RequestBody BulkRequest body) {
    return bulk.publish(scope(caller), body.problemIds());
  }

  @PostMapping("/bulk/unpublish")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  BulkProblemActionResult unpublish(JwtAuthenticationToken caller, @RequestBody BulkRequest body) {
    return bulk.unpublish(scope(caller), body.problemIds());
  }

  @PostMapping("/bulk/difficulty")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  BulkProblemActionResult difficulty(JwtAuthenticationToken caller, @RequestBody BulkRequest body) {
    return bulk.changeDifficulty(scope(caller), body.problemIds(), body.levelId());
  }

  @PostMapping("/bulk/topic")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:UPDATE')")
  BulkProblemActionResult topic(JwtAuthenticationToken caller, @RequestBody BulkRequest body) {
    return bulk.assignTopic(scope(caller), body.problemIds(), body.topicId());
  }

  @PostMapping("/bulk/delete")
  @PreAuthorize("hasAuthority('PROBLEM_AUTHORING:DELETE')")
  BulkProblemActionResult delete(JwtAuthenticationToken caller, @RequestBody BulkRequest body) {
    return bulk.delete(scope(caller), body.problemIds());
  }

  private static AuthoringScope scope(JwtAuthenticationToken caller) {
    return new AuthoringScope(
        UUID.fromString(caller.getToken().getSubject()),
        caller.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
  }
}
