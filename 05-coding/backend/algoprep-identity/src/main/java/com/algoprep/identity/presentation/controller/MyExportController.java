package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.AuditExport;
import com.algoprep.identity.application.dto.InterviewTranscriptExport;
import com.algoprep.identity.application.dto.PersonalDataExport;
import com.algoprep.identity.application.dto.SubmissionExport;
import com.algoprep.identity.application.ports.in.ExportMyInterviewTranscriptsUseCase;
import com.algoprep.identity.application.ports.in.ExportMyProfileDataUseCase;
import com.algoprep.identity.application.ports.in.ExportMySubmissionsUseCase;
import com.algoprep.identity.domain.model.MySettings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personal data export #16: {@code scope=profile} (JSON) and {@code scope=submissions} (CSV), and
 * #17: the interview transcripts as a JSON array.
 */
@RestController
@RequestMapping("/api/v1/identity/me/export")
class MyExportController {
  private static final List<String> SUBMISSION_HEADERS =
      List.of(
          "submission_id",
          "submitted_at",
          "problem_code",
          "problem_title",
          "language",
          "status",
          "passed_count",
          "total_count",
          "runtime_ms");

  private final ExportMyProfileDataUseCase exportProfile;
  private final ExportMySubmissionsUseCase exportSubmissions;
  private final ExportMyInterviewTranscriptsUseCase exportTranscripts;

  MyExportController(
      ExportMyProfileDataUseCase exportProfile,
      ExportMySubmissionsUseCase exportSubmissions,
      ExportMyInterviewTranscriptsUseCase exportTranscripts) {
    this.exportProfile = exportProfile;
    this.exportSubmissions = exportSubmissions;
    this.exportTranscripts = exportTranscripts;
  }

  record PersonalDataResponse(MyProfileController.ProfileResponse profile, MySettings settings) {}

  @GetMapping
  ResponseEntity<?> export(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String scope) {
    UUID caller = UUID.fromString(jwt.getSubject());
    if ("submissions".equals(scope)) {
      return submissions(exportSubmissions.export(caller));
    }
    if (!"profile".equals(scope)) {
      throw new InvalidRequestException("IDT-400", "scope must be profile or submissions");
    }
    PersonalDataExport data = exportProfile.export(caller);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"algoprep-profile.json\"")
        .body(
            new PersonalDataResponse(
                MyProfileController.ProfileResponse.of(data.profile()), data.settings()));
  }

  @GetMapping("/interview-transcripts")
  ResponseEntity<List<InterviewTranscriptExport.Session>> interviewTranscripts(
      @AuthenticationPrincipal Jwt jwt) {
    UUID caller = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"algoprep-interview-transcripts.json\"")
        .cacheControl(CacheControl.noStore())
        .body(exportTranscripts.export(caller).sessions());
  }

  private static ResponseEntity<byte[]> submissions(SubmissionExport data) {
    var lines =
        data.rows().stream()
            .map(
                r ->
                    List.of(
                        text(r.submissionId()),
                        text(r.submittedAt()),
                        text(r.problemCode()),
                        text(r.problemTitle()),
                        text(r.language()),
                        text(r.status()),
                        text(r.passedCount()),
                        text(r.totalCount()),
                        text(r.runtimeMs())))
            .iterator();
    var out = new ByteArrayOutputStream();
    try {
      AuditCsvWriter.write(new AuditExport(SUBMISSION_HEADERS, lines), out);
    } catch (IOException failure) {
      throw new UncheckedIOException(failure);
    }
    var response =
        ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"algoprep-submissions.csv\"")
            .cacheControl(CacheControl.noStore());
    if (data.truncated()) {
      response.header("X-Export-Truncated", "true");
    }
    return response.body(out.toByteArray());
  }

  private static String text(Object value) {
    return Objects.toString(value, "");
  }
}
