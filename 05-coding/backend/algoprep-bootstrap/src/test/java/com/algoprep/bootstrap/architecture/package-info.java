/**
 * Bộ test kiểm ranh giới module và ranh giới tầng.
 *
 * <p>Đặt ở {@code algoprep-bootstrap} vì đây là module duy nhất phụ thuộc cả tám module, nên là chỗ
 * duy nhất thấy được toàn bộ đồ hình để kiểm.
 *
 * <p>Đây là <strong>cách duy nhất</strong> ranh giới module được bảo vệ tự động. Không có nó thì
 * Modular Monolith trôi thành monolith phẳng sau vài sprint, và toàn bộ lý do chia module ở {@code
 * 01-rd/system/backend_architecture.md} mục 1 thành trang trí ({@code 01-rd/system/environment.md}
 * mục 3.A).
 */
package com.algoprep.bootstrap.architecture;
