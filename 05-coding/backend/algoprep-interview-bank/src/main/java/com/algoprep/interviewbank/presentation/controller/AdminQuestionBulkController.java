package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.interviewbank.application.ports.in.BulkRetireQuestionsUseCase;
import com.algoprep.interviewbank.application.ports.in.ImportQuestionsUseCase;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** DD api F6 sections 23 (bulk soft delete) and 24 (CSV import), Function cell only. */
@RestController
@RequestMapping("/api/v1/interview-bank/admin/questions")
class AdminQuestionBulkController {
  private final BulkRetireQuestionsUseCase retire;
  private final ImportQuestionsUseCase importer;

  AdminQuestionBulkController(BulkRetireQuestionsUseCase retire, ImportQuestionsUseCase importer) {
    this.retire = retire;
    this.importer = importer;
  }

  record Failure(UUID id, String errorCode) {}

  record Retired(List<UUID> retiredIds, List<Failure> failedItems) {}

  record Skipped(int line, String reasonCode, String detail) {}

  record Imported(int importedCount, List<Skipped> skippedRows) {}

  @PostMapping("/bulk/delete")
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:DELETE')")
  Retired retire(JwtAuthenticationToken caller, HttpServletRequest request) throws IOException {
    var body = QuestionRequests.body(request, QuestionRequests.QUESTION_BODY_LIMIT, "INB-V02");
    var result = retire.execute(QuestionRequests.bulk(caller, body));
    return new Retired(
        result.retiredIds(),
        result.failedIds().stream().map(id -> new Failure(id, "INB-010")).toList());
  }

  @PostMapping("/import")
  @PreAuthorize("hasAuthority('INTERVIEW_BANK_MANAGEMENT:CREATE')")
  Imported importRows(JwtAuthenticationToken caller, HttpServletRequest request)
      throws IOException {
    var body = QuestionRequests.body(request, QuestionRequests.IMPORT_BODY_LIMIT, "INB-V15");
    var result = importer.execute(QuestionRequests.importRows(caller, body));
    return new Imported(
        result.importedCount(),
        result.skippedRows().stream()
            .map(s -> new Skipped(s.line(), s.reasonCode(), s.detail()))
            .toList());
  }
}
