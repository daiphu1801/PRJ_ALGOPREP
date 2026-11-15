package com.algoprep.aireview.application.ports.in.practice;

import com.algoprep.aireview.application.dto.practice.PracticeFeedback;

/** P5: gives a reserved hourly slot back; a second call with the same ticket is a no-op. */
public interface RefundPracticeFeedbackUseCase {
  void refund(PracticeFeedback.Ticket ticket);
}
