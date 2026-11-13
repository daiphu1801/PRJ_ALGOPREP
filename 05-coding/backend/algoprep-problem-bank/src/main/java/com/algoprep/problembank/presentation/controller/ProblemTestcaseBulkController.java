package com.algoprep.problembank.presentation.controller;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.BulkTestcaseUpload;
import com.algoprep.problembank.application.dto.ReferenceRunReport;
import com.algoprep.problembank.application.ports.in.BulkTestcaseUploadUseCase;
import com.algoprep.problembank.application.ports.in.RunReferenceSolutionUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bulk testcase upload and the sample-solution run, #45 to #47 (DD F2 sections 46 to 48). An id
 * that is not a UUID is the same 404 as a missing problem or testcase; a malformed upload id is the
 * same 422 PRB-044 as an unknown one.
 */
@RestController
@RequestMapping("/api/v1/problem-bank/authoring/problems/{problemId}")
class ProblemTestcaseBulkController {
  private final BulkTestcaseUploadUseCase uploads;
  private final RunReferenceSolutionUseCase reference;

  ProblemTestcaseBulkController(
      BulkTestcaseUploadUseCase uploads, RunReferenceSolutionUseCase reference) {
    this.uploads = uploads;
    this.reference = reference;
  }

  record PairRequest(
      String clientKey,
      Long inputSizeBytes,
      Long expectedOutputSizeBytes,
      String visibility,
      String category) {}

  record UploadUrlRequest(List<PairRequest> items) {}

  record ConfirmRequest(String uploadId) {}

  record RunRequest(String testcaseId, String language, String sourceCode) {}

  @PostMapping("/testcases/bulk-upload-url")
  @PreAuthorize("hasAuthority('TESTCASE_MANAGEMENT:CREATE')")
  BulkTestcaseUpload.UrlPlan requestUrls(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody UploadUrlRequest body) {
    var items =
        body.items() == null
            ? null
            : body.items().stream()
                .map(
                    p ->
                        p == null
                            ? null
                            : new BulkTestcaseUpload.Item(
                                p.clientKey(),
                                p.inputSizeBytes() == null ? 0 : p.inputSizeBytes(),
                                p.expectedOutputSizeBytes() == null
                                    ? 0
                                    : p.expectedOutputSizeBytes(),
                                p.visibility(),
                                p.category()))
                .toList();
    return uploads.requestUrls(
        new BulkTestcaseUpload.RequestUrls(scope(caller), problem(problemId), items));
  }

  @PostMapping("/testcases/bulk-upload/confirm")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('TESTCASE_MANAGEMENT:CREATE')")
  BulkTestcaseUpload.Added confirm(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody ConfirmRequest body) {
    return uploads.confirm(
        new BulkTestcaseUpload.Confirm(scope(caller), problem(problemId), upload(body.uploadId())));
  }

  @PostMapping("/reference-solution/run")
  @PreAuthorize("hasAuthority('TESTCASE_MANAGEMENT:READ')")
  ReferenceRunReport.Report run(
      JwtAuthenticationToken caller,
      @PathVariable String problemId,
      @RequestBody(required = false) RunRequest body) {
    var request = body == null ? new RunRequest(null, null, null) : body;
    return reference.run(
        new ReferenceRunReport.Command(
            scope(caller),
            problem(problemId),
            request.testcaseId() == null ? null : testcase(request.testcaseId()),
            request.language(),
            request.sourceCode()));
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

  private static UUID upload(String raw) {
    try {
      return UUID.fromString(String.valueOf(raw));
    } catch (IllegalArgumentException e) {
      throw new DomainRuleViolationException("PRB-044", "Upload session not found");
    }
  }
}
