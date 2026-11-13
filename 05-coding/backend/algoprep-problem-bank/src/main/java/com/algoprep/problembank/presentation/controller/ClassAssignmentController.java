package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.problembank.application.dto.ClassAssignmentViews;
import com.algoprep.problembank.application.ports.in.ManageClassAssignmentsUseCase;
import com.algoprep.problembank.application.ports.in.ReadClassAssignmentsUseCase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instructor class assignments, #49 to #54 (DD F2 sections 50 to 55). The instructor is the token
 * subject. A class id that is not a UUID cannot be the caller's class, so it is the same 403 {@code
 * PRB-005}.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/instructor")
class ClassAssignmentController {
  private final ReadClassAssignmentsUseCase reads;
  private final ManageClassAssignmentsUseCase writes;

  ClassAssignmentController(
      ReadClassAssignmentsUseCase reads, ManageClassAssignmentsUseCase writes) {
    this.reads = reads;
    this.writes = writes;
  }

  record AssignRequest(List<UUID> problemIds) {}

  @GetMapping("/class-assignments")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  ClassAssignmentViews.AssignmentList list(
      JwtAuthenticationToken caller, @RequestParam Map<String, String> params) {
    return reads.list(user(caller), params);
  }

  @GetMapping("/class-assignments/summary")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  ClassAssignmentViews.Summary summary(JwtAuthenticationToken caller) {
    return reads.summary(user(caller));
  }

  @GetMapping("/classes/{classId}/assignable-problems")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  ClassAssignmentViews.AssignableList assignable(
      JwtAuthenticationToken caller,
      @PathVariable String classId,
      @RequestParam Map<String, String> params) {
    return reads.assignable(user(caller), classId(classId), params);
  }

  @PostMapping("/classes/{classId}/assignments")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:UPDATE')")
  ClassAssignmentViews.AssignResult assign(
      JwtAuthenticationToken caller,
      @PathVariable String classId,
      @RequestBody AssignRequest body) {
    return writes.assign(user(caller), classId(classId), body.problemIds());
  }

  @DeleteMapping("/classes/{classId}/assignments/{problemId}")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:UPDATE')")
  ResponseEntity<Void> remove(
      JwtAuthenticationToken caller, @PathVariable String classId, @PathVariable String problemId) {
    writes.remove(user(caller), classId(classId), problemOrNone(problemId));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/assigned-problems/top")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  ClassAssignmentViews.TopList top(JwtAuthenticationToken caller) {
    return reads.top(user(caller));
  }

  private static UUID user(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  private static UUID classId(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new PermissionDeniedException("PRB-005", "You are not responsible for this class");
    }
  }

  /** A problem id that is not a UUID was never assigned: the removal reports 409 PRB-025. */
  private static UUID problemOrNone(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      return new UUID(0, 0);
    }
  }
}
