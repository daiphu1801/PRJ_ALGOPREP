package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordSystemAuditCommandHandlerTest {
  @Test
  void forwardsTheEntryAndNeverSwallowsTheTransactionFailure() {
    var port = mock(AuditLogPort.class);
    var entry =
        new AuditEntry(
            UUID.randomUUID(), "PROBLEM_LEVEL_CREATED", "PROBLEM_LEVEL", "id", null, "{}");
    var handler = new RecordSystemAuditCommandHandler(port);
    handler.record(entry);
    verify(port).record(entry);
    doThrow(new IllegalStateException("audit failure")).when(port).record(entry);
    assertThatThrownBy(() -> handler.record(entry)).isInstanceOf(IllegalStateException.class);
  }
}
