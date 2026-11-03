// The backend returns a stable error code, not a translated message — the frontend maps the
// code itself (see shared/i18n/use-api-error-message.ts). Never show `message`: it is for logs.
export class ApiError extends Error {
  readonly code: string;
  readonly status: number;
  /** Extra metadata the backend attaches to some errors, e.g. `restoreTicket` on IDT-104. */
  readonly details?: Record<string, unknown>;

  constructor(
    code: string,
    status: number,
    message?: string,
    details?: Record<string, unknown>,
  ) {
    super(message ?? code);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
    this.details = details;
  }
}

/** True for a 404 from the server (also what A2 gets for a problem that is not theirs). */
export function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}

/**
 * Error body as sent by the server. The backend returns RFC 9457 `ProblemDetail` with a flat
 * `errorCode` (GlobalExceptionHandler); the DD documents a nested `error.code`
 * (03-dd/api/F1_identity.md:14). Both are read so neither side has to change first; the owner
 * still has to pick one as the contract.
 */
export type ErrorBody = {
  errorCode?: unknown;
  details?: unknown;
  error?: { code?: unknown; details?: unknown };
};

function asRecord(value: unknown): Record<string, unknown> | undefined {
  return typeof value === "object" && value !== null && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : undefined;
}

/** Builds an ApiError from an HTTP status and a response body of unknown shape. */
export function toApiError(
  status: number,
  body: ErrorBody | undefined,
  message?: string,
): ApiError {
  const code = [body?.errorCode, body?.error?.code].find(
    (candidate): candidate is string =>
      typeof candidate === "string" && candidate.length > 0,
  );
  const details = asRecord(body?.details) ?? asRecord(body?.error?.details);
  return new ApiError(code ?? "UNKNOWN_ERROR", status, message, details);
}
