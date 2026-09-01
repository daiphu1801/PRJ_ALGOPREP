package com.algoprep.common.exception;

/**
 * Gốc của mọi ngoại lệ nghiệp vụ trong AlgoPrep.
 *
 * <p>Mang một <strong>mã lỗi ổn định</strong> tách khỏi thông điệp. Frontend dịch theo mã, không
 * bao giờ hiển thị message thô ({@code 01-rd/system/backend_architecture.md} mục 7) — nên message ở
 * đây viết cho log và cho lập trình viên, không viết cho người dùng cuối.
 *
 * <p><strong>Danh mục mã lỗi chưa được chốt.</strong> Nó thuộc {@code 03-dd/api/api.md}, tài liệu
 * chưa tồn tại. Ở khung base, mã lỗi là một {@code String} do nơi ném tự đặt. Khi danh mục được
 * chốt, đổi tham số này thành một enum — đó là lý do nó được tách thành trường riêng ngay từ đầu
 * thay vì nhét vào message.
 */
public abstract class AlgoPrepException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String errorCode;

  protected AlgoPrepException(String errorCode, String message) {
    super(message);
    this.errorCode = requireCode(errorCode);
  }

  protected AlgoPrepException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = requireCode(errorCode);
  }

  /**
   * Mã lỗi ổn định để tầng ngoài ánh xạ sang phản hồi HTTP và để frontend dịch.
   *
   * @return mã lỗi, không bao giờ {@code null} hay rỗng
   */
  public String errorCode() {
    return errorCode;
  }

  private static String requireCode(String errorCode) {
    if (errorCode == null || errorCode.isBlank()) {
      throw new IllegalArgumentException("errorCode không được rỗng");
    }
    return errorCode;
  }
}
