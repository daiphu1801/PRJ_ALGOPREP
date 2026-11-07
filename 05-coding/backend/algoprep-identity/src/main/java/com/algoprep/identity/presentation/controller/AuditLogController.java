package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.AuditExport;
import com.algoprep.identity.application.dto.AuditLogPageResult;
import com.algoprep.identity.application.dto.CategoryCount;
import com.algoprep.identity.application.dto.ExportAuditLogsQuery;
import com.algoprep.identity.application.dto.SearchAuditLogsQuery;
import com.algoprep.identity.application.ports.in.ExportSystemAuditLogsUseCase;
import com.algoprep.identity.application.ports.in.GetAuditCategoryBreakdownUseCase;
import com.algoprep.identity.application.ports.in.ListActiveAdminsUseCase;
import com.algoprep.identity.application.ports.in.SearchSystemAuditLogsUseCase;
import com.algoprep.identity.domain.model.ActiveAdmin;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * System audit log read endpoints #39, #40, #64, #65 (DD api F1 section 0.7). Any ADMIN reads them;
 * there is no permission cell (the {@code SYSTEM_AUDIT_LOG} cell only guards editing the retention
 * settings).
 */
@RestController
@RequestMapping("/api/v1/identity/admin/audit-logs")
class AuditLogController {
  private static final Logger LOG = LoggerFactory.getLogger(AuditLogController.class);
  private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

  private final SearchSystemAuditLogsUseCase search;
  private final ExportSystemAuditLogsUseCase export;
  private final ListActiveAdminsUseCase activeAdmins;
  private final GetAuditCategoryBreakdownUseCase breakdown;

  AuditLogController(
      SearchSystemAuditLogsUseCase search,
      ExportSystemAuditLogsUseCase export,
      ListActiveAdminsUseCase activeAdmins,
      GetAuditCategoryBreakdownUseCase breakdown) {
    this.search = search;
    this.export = export;
    this.activeAdmins = activeAdmins;
    this.breakdown = breakdown;
  }

  record ActiveAdminsResponse(List<ActiveAdmin> items) {}

  record CategoryBreakdownResponse(List<CategoryCount> items) {}

  /** Query parameters of #40; absent ones stay null and the handler validates them. */
  record ExportParams(
      String q,
      String categoryCode,
      String dateFrom,
      String dateTo,
      List<String> columns,
      String format) {}

  /** The query parameters bind straight into the use-case query record. */
  @GetMapping
  AuditLogPageResult search(Authentication caller, SearchAuditLogsQuery query) {
    AdminOnly.require(caller);
    return search.search(query);
  }

  @GetMapping("/export")
  ResponseEntity<StreamingResponseBody> export(
      Authentication caller,
      ExportParams params,
      @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String language) {
    AdminOnly.require(caller);
    AuditExport prepared =
        export.open(
            new ExportAuditLogsQuery(
                UUID.fromString(caller.getName()),
                params.q(),
                params.categoryCode(),
                params.dateFrom(),
                params.dateTo(),
                params.columns(),
                params.format(),
                language));
    StreamingResponseBody body =
        out -> {
          try {
            AuditCsvWriter.write(prepared, out);
          } catch (IOException | RuntimeException failure) {
            // The status line is gone by now, so the only honest answer is to abort the transfer
            // and log the cause without any row content (DD 0.7.2).
            LOG.error("Audit log export aborted: {}", failure.getClass().getSimpleName());
            throw failure;
          }
        };
    String name =
        "audit-logs-" + FILE_STAMP.format(LocalDateTime.now(ZoneOffset.ofHours(7))) + ".csv";
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
        .cacheControl(CacheControl.noStore())
        .body(body);
  }

  @GetMapping("/active-admins")
  ActiveAdminsResponse activeAdmins(Authentication caller) {
    AdminOnly.require(caller);
    return new ActiveAdminsResponse(activeAdmins.list());
  }

  @GetMapping("/category-breakdown")
  CategoryBreakdownResponse categoryBreakdown(Authentication caller) {
    AdminOnly.require(caller);
    return new CategoryBreakdownResponse(breakdown.get());
  }
}
