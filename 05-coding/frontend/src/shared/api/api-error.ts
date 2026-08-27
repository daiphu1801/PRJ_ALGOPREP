// Backend trả mã lỗi ổn định, không trả message đã dịch — frontend tự ánh xạ mã lỗi
// (xem shared/i18n + useErrorMessage, sẽ dựng khi có 03-dd/api thật).
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
