/**
 * Điểm khởi chạy của backend AlgoPrep.
 *
 * <p>Không phải một Bounded Context. Chỉ chứa việc khởi chạy, cấu hình toàn cục, và bộ test kiểm
 * ranh giới module. Logic nghiệp vụ đặt ở đây là cách một modular monolith âm thầm trở thành
 * monolith phẳng ({@code 01-rd/system/backend_architecture.md} mục 2).
 */
package com.algoprep.bootstrap;
