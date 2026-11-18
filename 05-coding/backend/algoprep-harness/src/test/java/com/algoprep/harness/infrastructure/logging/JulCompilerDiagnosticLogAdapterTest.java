package com.algoprep.harness.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JulCompilerDiagnosticLogAdapterTest {
  @Test
  void recordsTheBoundedPayloadAtErrorLevelWithCorrelationAndTruncation() {
    Logger logger = Logger.getLogger(JulCompilerDiagnosticLogAdapter.class.getName());
    Handler sink = mock(Handler.class);
    boolean parent = logger.getUseParentHandlers();
    logger.setUseParentHandlers(false);
    logger.addHandler(sink);
    try {
      new JulCompilerDiagnosticLogAdapter().write("case-1", "bounded stderr", true);
      ArgumentCaptor<LogRecord> record = ArgumentCaptor.forClass(LogRecord.class);
      verify(sink).publish(record.capture());
      assertThat(record.getValue().getLevel()).isEqualTo(Level.SEVERE);
      assertThat(record.getValue().getParameters())
          .containsExactly("case-1", true, "bounded stderr");
    } finally {
      logger.removeHandler(sink);
      logger.setUseParentHandlers(parent);
    }
  }
}
