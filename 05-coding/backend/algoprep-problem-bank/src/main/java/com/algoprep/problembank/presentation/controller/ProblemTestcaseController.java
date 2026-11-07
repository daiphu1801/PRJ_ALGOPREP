package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.TestcaseCommands;
import com.algoprep.problembank.application.dto.TestcaseMutationResults;
import com.algoprep.problembank.application.ports.in.AuthorTestcasesUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * Inline testcase authoring, #41 to #44 (DD F2 sections 42 to 45). The caller is the token subject;
 * an id that is not a UUID is the same 404 as a missing problem or testcase.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/authoring/problems/{problemId}/testcases")
class ProblemTestcaseController {
  private final AuthorTestcasesUseCase testcases;

  ProblemTestcaseController(AuthorTestcasesUseCase testcases) {
    this.testcases = testcases;
  }

  record CreateRequest(String visibility, String input, String expectedOutput, String category) {}

  record OrderRequest(List<UUID> orderedIds) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('TESTCASE_MANAGEMENT:CREATE')")
  TestcaseMutationResults.Saved create(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody CreateRequest body) {
    return testcases.create(
        new TestcaseCommands.Create(
            scope(caller),
            problem(problemId),
            body.visibility(),
            body.input(),
            body.expectedOutput(),
            body.category()));
  }

  @PatchMapping("/{testcaseId}")
  @PreAuthorize("hasAuthority('TESTCASE_MANAGEMENT:UPDATE')")
  TestcaseMutationResults.Saved update(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @PathVariable String testcaseId,
      @RequestBody JsonNode body) {
    return testcases.update(
        new TestcaseCommands.Update(
            scope(caller), problem(problemId), testcase(testcaseId), patch(body)));
  }

  @DeleteMapping("/{testcaseId}")
  @PreAuthorize("hasAuthority('TESTCASE_MANAGEMENT:DELETE')")
  TestcaseMutationResults.Removed delete(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @PathVariable String testcaseId) {
    return testcases.delete(
        new TestcaseCommands.Delete(scope(caller), problem(problemId), testcase(testcaseId)));
  }

  @PutMapping("/order")
  @PreAuthorize("hasAuthority('TESTCASE_MANAGEMENT:UPDATE')")
  TestcaseMutationResults.Ordered reorder(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody OrderRequest body) {
    return testcases.reorder(
        new TestcaseCommands.Reorder(scope(caller), problem(problemId), body.orderedIds()));
  }

  /** Only the keys present are changed; a wrongly typed value is 400 PRB-001. */
  private static TestcaseCommands.Patch patch(JsonNode body) {
    if (body == null || !body.isObject()) {
      throw invalid();
    }
    return new TestcaseCommands.Patch(
        text(body, "visibility"),
        text(body, "input"),
        text(body, "expectedOutput"),
        body.has("category"),
        text(body, "category"),
        flag(body, "approved").orElse(null));
  }

  private static String text(JsonNode body, String key) {
    JsonNode value = body.get(key);
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isString()) {
      throw invalid();
    }
    return value.asString();
  }

  private static Optional<Boolean> flag(JsonNode body, String key) {
    JsonNode value = body.get(key);
    if (value == null || value.isNull()) {
      return Optional.empty();
    }
    if (!value.isBoolean()) {
      throw invalid();
    }
    return Optional.of(value.asBoolean());
  }

  private static InvalidRequestException invalid() {
    return new InvalidRequestException("PRB-001", "Invalid testcase request");
  }

  private static AuthoringScope scope(JwtAuthenticationToken caller) {
    boolean admin =
        caller.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    return new AuthoringScope(UUID.fromString(caller.getToken().getSubject()), admin);
  }

  private static UUID problem(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException("PRB-010", "Problem not found");
    }
  }

  private static UUID testcase(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException("PRB-011", "Testcase not found");
    }
  }
}
