// The backend returns a stable error code, not a translated message — the frontend maps the
// code itself (see shared/i18n + useErrorMessage, to be built once a real 03-dd/api exists).
export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(code: string, status: number, message?: string) {
    super(message ?? code);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

/** True for a 404 from the server (also what A2 gets for a problem that is not theirs). */
export function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}
