package com.algoprep.judge.application.dto;

/**
 * Internal result, not an HTTP response. A future caller emits the final STOMP notice only when
 * queueUnavailable() is true, then maps it to JDG-503 with ticket.submissionId. Otherwise it
 * returns a ticket and lets progress reads report the current worker state.
 */
public record QueuedSubmissionResult(
    PendingSubmissionTicket ticket, QueueFailureResolution queueFailure) {
  public boolean queueUnavailable() {
    return queueFailure != null && queueFailure.finalized();
  }

  public String currentStatus() {
    return queueFailure == null ? ticket.status() : queueFailure.submission().status();
  }
}
