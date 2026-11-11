package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.SendInterviewTurnCommand;
import com.algoprep.aireview.application.ports.out.InterviewTurnSink;

/**
 * Endpoint 10. Blocks until the turn is over. Every failure before {@code accepted} is thrown;
 * every failure after it goes to {@link InterviewTurnSink#failed}.
 */
public interface SendInterviewTurnUseCase {
  void send(SendInterviewTurnCommand command, InterviewTurnSink sink);
}
