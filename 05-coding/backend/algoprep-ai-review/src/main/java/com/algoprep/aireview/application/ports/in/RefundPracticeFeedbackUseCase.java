package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.PracticeFeedback;

/** P5: gives a reserved hourly slot back; a second call with the same ticket is a no-op. */
public interface RefundPracticeFeedbackUseCase {
  void refund(PracticeFeedback.Ticket ticket);
}
