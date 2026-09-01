package com.algoprep.common.exception;

/**
 * Người gọi đã xác thực nhưng không có quyền thực hiện hành động này.
 *
 * <p>Khác với việc kiểm quyền ở tầng giao diện: quyền sở hữu dữ liệu phải kiểm ở tầng {@code
 * application} của từng module, vì "kiểm quyền chỉ ở giao diện là không kiểm" ({@code
 * 01-rd/system/backend_architecture.md} mục 7).
 */
public class PermissionDeniedException extends AlgoPrepException {

  private static final long serialVersionUID = 1L;

  public PermissionDeniedException(String errorCode, String message) {
    super(errorCode, message);
  }
}
