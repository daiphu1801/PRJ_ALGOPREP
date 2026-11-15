package com.algoprep.identity.application.query;

import com.algoprep.identity.application.dto.AuditLogItem;
import com.algoprep.identity.application.dto.AuditLogPageResult;
import com.algoprep.identity.application.dto.SearchAuditLogsQuery;
import com.algoprep.identity.application.ports.in.SearchSystemAuditLogsUseCase;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogFilter;
import com.algoprep.identity.domain.model.AuditLogRow;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #39: one keyset page of 50 rows. Fetches 51 to know whether another page exists, so the cursor is
 * issued only when there is a next row and the total is never guessed.
 */
@Service
class SearchSystemAuditLogsQueryHandler implements SearchSystemAuditLogsUseCase {
  static final int PAGE_SIZE = 50;

  private final AuditLogFilterParser filters;
  private final AuditLogReadRepository logs;

  SearchSystemAuditLogsQueryHandler(AuditLogFilterParser filters, AuditLogReadRepository logs) {
    this.filters = filters;
    this.logs = logs;
  }

  @Override
  @Transactional(readOnly = true)
  public AuditLogPageResult search(SearchAuditLogsQuery query) {
    AuditLogFilter filter =
        filters.parse(query.q(), query.categoryCode(), query.dateFrom(), query.dateTo());
    AuditLogCursor after =
        query.cursor() == null || query.cursor().isBlank()
            ? null
            : AuditCursorCodec.decode(query.cursor(), filter);
    List<AuditLogRow> rows = logs.search(filter, after, PAGE_SIZE + 1);
    boolean more = rows.size() > PAGE_SIZE;
    List<AuditLogRow> page = more ? rows.subList(0, PAGE_SIZE) : rows;
    List<AuditLogItem> items = page.stream().map(AuditLogItemMapper::toItem).toList();
    String next =
        more
            ? AuditCursorCodec.encode(AuditLogItemMapper.cursorOf(page.get(PAGE_SIZE - 1)), filter)
            : null;
    return new AuditLogPageResult(items, next);
  }
}
