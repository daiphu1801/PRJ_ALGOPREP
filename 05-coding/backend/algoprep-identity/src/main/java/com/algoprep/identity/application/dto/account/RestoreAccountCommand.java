package com.algoprep.identity.application.dto;

/** The ticket never belongs in logs or URLs. */
public record RestoreAccountCommand(String restoreTicket) {
  @Override
  public String toString() {
    return "RestoreAccountCommand[redacted]";
  }
}
