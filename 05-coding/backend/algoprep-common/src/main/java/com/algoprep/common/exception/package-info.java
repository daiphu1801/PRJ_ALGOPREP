/**
 * Ngoại lệ nền của toàn hệ thống.
 *
 * <p>Mọi ngoại lệ nghiệp vụ của sáu Bounded Context kế thừa từ {@link
 * com.algoprep.common.exception.AlgoPrepException}, nhờ đó bộ xử lý lỗi toàn cục ở {@code
 * algoprep-bootstrap} ánh xạ được một chỗ sang mã lỗi ổn định ({@code
 * 01-rd/system/backend_architecture.md} mục 7).
 */
package com.algoprep.common.exception;
