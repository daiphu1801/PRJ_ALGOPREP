package com.algoprep.identity.application.query;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.AuditExport;
import com.algoprep.identity.application.dto.AuditLogItem;
import com.algoprep.identity.application.dto.ExportAuditLogsQuery;
import com.algoprep.identity.application.ports.in.ExportSystemAuditLogsUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogFilter;
import com.algoprep.identity.domain.model.AuditLogRow;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * #40: validates, records the export in the log, then hands back a lazy row iterator. This is the
 * one handler under {@code query} that writes: the export action itself is an audit row (BD ADM0403
 * Sheet 8 EVT-12), written in this transaction before any body byte is sent. The row pages are read
 * afterwards, one keyset page at a time, outside the transaction.
 */
@Service
class ExportSystemAuditLogsQueryHandler implements ExportSystemAuditLogsUseCase {
  static final int PAGE = 500;
  static final String ACTION = "AUDIT_LOG_EXPORTED";

  private static final DateTimeFormatter TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final ZoneOffset LOCAL = ZoneOffset.ofHours(7);
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private record Column(String key, String vi, String en, Function<Cell, String> value) {}

  private record Cell(AuditLogItem item, boolean english) {}

  private static final List<Column> COLUMNS =
      List.of(
          new Column(
              "time", "Thời gian", "Time", c -> TIME.format(c.item().createdAt().atOffset(LOCAL))),
          new Column("actor", "Người thực hiện", "Actor", c -> c.item().actorEmail()),
          new Column("service", "Dịch vụ", "Service", c -> c.item().serviceCode()),
          new Column("category", "Phân loại", "Category", c -> c.item().categoryCode()),
          new Column(
              "content", "Nội dung", "Content", c -> AuditMessages.sentence(c.item(), c.english())),
          new Column("id", "Mã sự kiện", "Event ID", c -> c.item().id().toString()));

  private static final Set<String> KEYS =
      Set.of("time", "actor", "service", "category", "content", "id");

  private final AuditLogFilterParser filters;
  private final AuditLogReadRepository logs;
  private final AuditLogPort audit;

  ExportSystemAuditLogsQueryHandler(
      AuditLogFilterParser filters, AuditLogReadRepository logs, AuditLogPort audit) {
    this.filters = filters;
    this.logs = logs;
    this.audit = audit;
  }

  @Override
  @Transactional
  public AuditExport open(ExportAuditLogsQuery query) {
    AuditLogFilter filter =
        filters.parse(query.q(), query.categoryCode(), query.dateFrom(), query.dateTo());
    List<Column> chosen = columns(query.columns());
    if (query.format() != null && !"csv".equalsIgnoreCase(query.format())) {
      throw invalid("Only csv is supported");
    }
    boolean english = isEnglish(query.acceptLanguage());
    audit.record(
        new AuditEntry(
            query.actorUserId(), ACTION, "AUDIT_LOG", null, null, snapshot(query, chosen)));
    List<String> headers = chosen.stream().map(c -> english ? c.en() : c.vi()).toList();
    return new AuditExport(headers, new PagedRows(logs, filter, chosen, english));
  }

  private static List<Column> columns(List<String> requested) {
    if (requested == null || requested.isEmpty()) {
      throw invalid("At least one column is required");
    }
    if (new java.util.HashSet<>(requested).size() != requested.size()
        || !KEYS.containsAll(requested)) {
      throw invalid("Columns must be unique and known");
    }
    List<Column> chosen = new ArrayList<>();
    for (String key : requested) {
      chosen.add(COLUMNS.stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow());
    }
    return chosen;
  }

  private static boolean isEnglish(String acceptLanguage) {
    return acceptLanguage != null
        && acceptLanguage.trim().toLowerCase(Locale.ROOT).startsWith("en");
  }

  private static String snapshot(ExportAuditLogsQuery query, List<Column> chosen) {
    Map<String, Object> after = new LinkedHashMap<>();
    after.put("columns", chosen.stream().map(Column::key).toList());
    putIfPresent(after, "q", query.q());
    putIfPresent(after, "categoryCode", query.categoryCode());
    putIfPresent(after, "dateFrom", query.dateFrom());
    putIfPresent(after, "dateTo", query.dateTo());
    return JSON.writeValueAsString(after);
  }

  private static void putIfPresent(Map<String, Object> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value.trim());
    }
  }

  private static InvalidRequestException invalid(String message) {
    return new InvalidRequestException(IdentityErrorCodes.AUDIT_FILTER_INVALID, message);
  }

  /** Reads the log in keyset pages of {@link #PAGE} rows and renders each row to cells. */
  private static final class PagedRows implements Iterator<List<String>> {
    private final AuditLogReadRepository logs;
    private final AuditLogFilter filter;
    private final List<Column> columns;
    private final boolean english;
    private final Deque<List<String>> buffer = new ArrayDeque<>();
    private AuditLogCursor cursor;
    private boolean exhausted;

    PagedRows(
        AuditLogReadRepository logs, AuditLogFilter filter, List<Column> columns, boolean english) {
      this.logs = logs;
      this.filter = filter;
      this.columns = columns;
      this.english = english;
    }

    @Override
    public boolean hasNext() {
      if (buffer.isEmpty() && !exhausted) {
        load();
      }
      return !buffer.isEmpty();
    }

    @Override
    public List<String> next() {
      if (!hasNext()) {
        throw new java.util.NoSuchElementException();
      }
      return buffer.removeFirst();
    }

    private void load() {
      List<AuditLogRow> rows = logs.search(filter, cursor, PAGE);
      exhausted = rows.size() < PAGE;
      for (AuditLogRow row : rows) {
        Cell cell = new Cell(AuditLogItemMapper.toItem(row), english);
        buffer.add(columns.stream().map(c -> c.value().apply(cell)).toList());
      }
      if (!rows.isEmpty()) {
        cursor = AuditLogItemMapper.cursorOf(rows.get(rows.size() - 1));
      }
    }
  }
}
