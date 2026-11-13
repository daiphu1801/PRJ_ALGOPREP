package com.algoprep.bootstrap.interviewbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.algoprep.aireview.application.ports.in.ComparePracticeAnswerUseCase;
import com.algoprep.aireview.application.ports.in.RefundPracticeFeedbackUseCase;
import com.algoprep.aireview.application.ports.in.ReservePracticeFeedbackUseCase;
import com.algoprep.interviewbank.domain.exception.FeedbackUnavailableException;
import com.algoprep.interviewbank.domain.model.FeedbackRequest;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** A swallowed AI failure must leave a diagnosable WARN line without any learner text. */
class AnswerFeedbackBridgeLogTest {
  @Test
  void aProviderFailureIsLoggedAtWarnWithClassAndMessageButNeverTheAnswer() {
    var compare = mock(ComparePracticeAnswerUseCase.class);
    when(compare.compare(any())).thenThrow(new IllegalStateException("boom-detail"));
    var bridge =
        new AnswerFeedbackBridge(
            mock(ReservePracticeFeedbackUseCase.class),
            mock(RefundPracticeFeedbackUseCase.class),
            compare);
    var request =
        new FeedbackRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "sys",
            List.of(new RubricCriterion("Clarity", new BigDecimal("100.00"), null)),
            "Title",
            "",
            "secret answer text");
    var logger = (Logger) LoggerFactory.getLogger(AnswerFeedbackBridge.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      assertThatThrownBy(() -> bridge.compareAnswer(request))
          .isInstanceOf(FeedbackUnavailableException.class);
    } finally {
      logger.detachAppender(appender);
    }
    assertThat(appender.list).hasSize(1);
    ILoggingEvent event = appender.list.get(0);
    assertThat(event.getLevel()).isEqualTo(Level.WARN);
    assertThat(event.getFormattedMessage())
        .contains("java.lang.IllegalStateException", "boom-detail")
        .doesNotContain("secret answer text");
    assertThat(event.getThrowableProxy()).isNull();
  }
}
