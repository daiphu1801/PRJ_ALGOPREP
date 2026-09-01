package com.algoprep.common.exception;

/**
 * Không tìm thấy tài nguyên được yêu cầu.
 *
 * <p>Dùng cho cả trường hợp tài nguyên không tồn tại và trường hợp người gọi không được phép biết
 * là nó tồn tại — hai trường hợp phải trả về cùng một phản hồi, nếu không thì phản hồi trở thành
 * một kênh dò dữ liệu.
 */
public class ResourceNotFoundException extends AlgoPrepException {

  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(String errorCode, String message) {
    super(errorCode, message);
  }
}
