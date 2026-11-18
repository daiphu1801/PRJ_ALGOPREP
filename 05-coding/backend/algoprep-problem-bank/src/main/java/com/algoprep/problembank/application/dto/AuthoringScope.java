package com.algoprep.problembank.application.dto;

import java.util.UUID;

/** Whose problems an authoring read covers: an instructor's own, or the whole bank for an admin. */
public record AuthoringScope(UUID actorId, boolean admin) {

  /** The author filter, or null for the whole bank. */
  public UUID authorFilter() {
    return admin ? null : actorId;
  }
}
