/**
 * Handler CHỈ đọc.
 *
 * <p>Không transaction ghi. Tách khỏi command/ vì hai đường có nhu cầu dữ liệu khác nhau — KHÔNG có
 * package application/usecase (01-rd/system/backend_architecture.md mục 3.A).
 */
package com.algoprep.problembank.application.query;
