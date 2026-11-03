package com.algoprep.identity.domain.exception;

/**
 * Error codes of the audit-log read side that {@link IdentityErrorCodes} does not carry. Kept in
 * its own class so parallel slices do not edit the same file.
 */
public final class AuditReadErrorCodes {

  /**
   * Too many live viewers of the audit stream on this instance (HTTP 429). The DD lists no code for
   * the capacity ceiling of #66; {@code IDT-449} is the next free one {@code [SoT: Suy luan]}.
   */
  public static final String AUDIT_STREAM_CAPACITY = "IDT-449";

  private AuditReadErrorCodes() {}
}
