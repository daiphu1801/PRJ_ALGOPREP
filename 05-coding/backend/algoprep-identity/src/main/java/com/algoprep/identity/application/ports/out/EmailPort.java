package com.algoprep.identity.application.ports.out;

/** Sends a plain-text transactional email; delivery failures propagate to the caller. */
public interface EmailPort {
  void send(String recipient, String subject, String body);
}
