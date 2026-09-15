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
