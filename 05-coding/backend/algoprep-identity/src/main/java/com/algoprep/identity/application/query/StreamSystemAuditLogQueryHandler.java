package com.algoprep.identity.application.query;

import com.algoprep.identity.application.dto.AuditLogItem;
import com.algoprep.identity.application.dto.AuditStreamBatch;
import com.algoprep.identity.application.dto.AuditStreamRequest;
import com.algoprep.identity.application.ports.in.StreamSystemAuditLogUseCase;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogRow;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #66 data source: rows newer than a position, masked like #39. A reconnect marker that is gone or
 * older than the retention window answers {@code resync} instead of an error (DD 0.7.2).
 */
@Service
class StreamSystemAuditLogQueryHandler implements StreamSystemAuditLogUseCase {
  private final AuditLogReadRepository logs;

  StreamSystemAuditLogQueryHandler(AuditLogReadRepository logs) {
    this.logs = logs;
  }

  @Override
  @Transactional(readOnly = true)
  public AuditStreamBatch next(AuditStreamRequest request) {
    if (request.after() != null) {
      return after(request.after(), request);
    }
    if (request.lastEventId() != null) {
      Optional<AuditLogCursor> marker = logs.findWithinRetention(request.lastEventId());
      return marker.isPresent()
          ? after(marker.get(), request)
          : new AuditStreamBatch(List.of(), null, true);
    }
    return new AuditStreamBatch(List.of(), logs.latest().orElse(null), false);
  }

  private AuditStreamBatch after(AuditLogCursor start, AuditStreamRequest request) {
    List<AuditLogRow> rows = logs.newer(start, request.upTo(), request.limit());
    List<AuditLogItem> items = rows.stream().map(AuditLogItemMapper::toItem).toList();
    AuditLogCursor position =
        rows.isEmpty() ? start : AuditLogItemMapper.cursorOf(rows.get(rows.size() - 1));
    return new AuditStreamBatch(items, position, false);
  }
}
