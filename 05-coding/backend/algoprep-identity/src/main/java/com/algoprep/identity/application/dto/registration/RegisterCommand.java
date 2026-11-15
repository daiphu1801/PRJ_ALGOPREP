package com.algoprep.identity.application.dto;

/** Input of self-registration; no client-supplied role or lifecycle state. */
public record RegisterCommand(String email, String password, String displayName) {
  @Override
  public String toString() {
    return "RegisterCommand[redacted]";
  }
}
